package com.cableops.tracker.service;

import com.cableops.tracker.dto.*;
import com.cableops.tracker.entity.*;
import com.cableops.tracker.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository taskRepo;
	private final TaskCommentRepository taskCommentRepo;
	private final TaskCircuitRepository taskCircuitRepo;
	private final TaskEbbMllRepository taskEbbMllRepo;
	private final OhfCircuitRepository circuitRepo;
	private final EbbMllRepository ebbMllRepo;
	private final UserRepository userRepo;
	private final UserTeamRepository userTeamRepo;
	private final TeamRepository teamRepo;
	private final AccountRepository accountRepo;
	private final AttachmentRepository attachmentRepo;
	private final TelegramService telegramService;
	private final ObjectMapper objectMapper;
	private final InventoryStockRepository inventoryStockRepo;
	private final InventoryTransactionRepository inventoryTxnRepo; // ← typo fixed

	private static final String JCV_MEDIUM = "jcv-md";
	private static final String JCV_SMALL = "jcv-sm";

	@Value("${telegram.admin.group.chatId:}")
	private String adminGroupChatId;

	// ── ACCEPT ────────────────────────────────────────────────────────────────
	@Transactional
	public TaskResponse acceptTask(String id) {
		Task task = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found: " + id));

		if (task.getAcceptedAt() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task has already been accepted");
		}

		String userId = currentUserId();
		String userName = currentUserName();

		task.setAcceptedAt(LocalDateTime.now());
		task.setAcceptedById(userId);
		task.setAcceptedByName(userName);
		task.setStatus("Accepted");
		task.setModifiedAt(LocalDateTime.now());
		taskRepo.save(task);

		try {
			String data = objectMapper.writeValueAsString(Map.of("from", "Asigned", "to", "Accepted"));
			TaskComment comment = new TaskComment();
			comment.setId(UUID.randomUUID().toString());
			comment.setTaskId(task.getId());
			comment.setUserId(userId);
			comment.setUserName(userName);
			comment.setType("status");
			comment.setData(data);
			comment.setCreatedAt(LocalDateTime.now());
			taskCommentRepo.save(comment);
		} catch (Exception e) {
			log.warn("Failed to log accept status change: {}", e.getMessage());
		}

		// ── Send Telegram notifications ───────────────────────────────────────
		try {
			// Full detail → assigned users
			String acceptUserMsg = buildAcceptMessage(task, userName);
			notifyUser(task.getAssignedUserId(), acceptUserMsg);
			notifyAdminGroup(acceptUserMsg); // ← ADD

			if (!isBlank(task.getCSecondaryUserIds())) {
				for (String sid : task.getCSecondaryUserIds().split(",")) {
					if (!isBlank(sid))
						notifyUser(sid.trim(), acceptUserMsg);
				}
			} else if (task.getCSecondaryAssignedUserId() != null) {
				notifyUser(task.getCSecondaryAssignedUserId(), acceptUserMsg);
			}

			// Compact status → account group chat only
			String acceptGroupMsg = buildGroupStatusMessage("📌 Task Update", task.getCSRNumber(), task.getParentName(),
					"Accepted", null);
			notifyAccountGroup(task.getAccountId(), acceptGroupMsg);

		} catch (Exception e) {
			log.error("Accept Telegram failed task {}: {}", id, e.getMessage(), e);
		}
		return toResponseFromDb(task);
	}

	private void notifyAdminGroup(String message) {
		userRepo.findByType("ADMIN").forEach(admin -> {
			String chatId = admin.getTelegramUsername();
			if (chatId != null && !chatId.isBlank()) {
				log.info("Notifying admin userId={} chatId={}", admin.getId(), chatId);
				telegramService.sendMessage(chatId, message);
			}
		});
	}

	private String buildGroupStatusMessage(String header, String srNumber, String circuitName, String status,
			String extraLine) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b>").append(header).append("</b>\n\n");
		sb.append("<b>SR Number:</b> ").append(coalesce(srNumber, "—")).append("\n");
		sb.append("<b>Circuit Name:</b> ").append(coalesce(circuitName, "—")).append("\n");
		sb.append("<b>Status:</b> ").append(coalesce(status, "—")).append("\n");
		if (!isBlank(extraLine))
			sb.append(extraLine).append("\n");
		return sb.toString();
	}

	private String buildAcceptMessage(Task task, String acceptedByName) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b>✅ Task Accepted</b>\n\n");
		sb.append("<b>Circuit Name:</b> ").append(coalesce(task.getParentName(), "—")).append("\n");
		sb.append("<b>Account:</b> ").append(coalesce(task.getAccountName(), "—")).append("\n");
		sb.append("<b>SR Number:</b> ").append(coalesce(task.getCSRNumber(), "—")).append("\n");
		sb.append("<b>Work Type:</b> ").append(coalesce(task.getCWorkType(), "—")).append("\n");
		sb.append("<b>Priority:</b> ").append(coalesce(task.getPriority(), "Normal")).append("\n");
		if (!isBlank(task.getAssignedUserName()))
			sb.append("<b>Assigned To:</b> ").append(task.getAssignedUserName()).append("\n");
		sb.append("<b>Accepted By:</b> ").append(coalesce(acceptedByName, "—")).append("\n");
		sb.append("\n<i>Status: Assigned → Accepted</i>");
		return sb.toString();
	}

	public Task getTaskEntity(String id) {
		return taskRepo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
	}

	// ── CREATE ────────────────────────────────────────────────────────────────
	@Transactional
	public TaskResponse create(TaskRequest req) {
		Task t = new Task();
		t.setId(UUID.randomUUID().toString());
		t.setCSRNumber(generateSRNumber());
		t.setCreatedAt(LocalDateTime.now());
		t.setModifiedAt(LocalDateTime.now());
		map(t, req);

		// dateStart = creation timestamp always (not user input)
		t.setDateStart(t.getCreatedAt());
		t.setDateStartDate(t.getCreatedAt().toLocalDate());

		resolveAccount(t, req.getParentId(), req.getParentType(), req.getDescription());

		taskRepo.save(t);
		saveCircuits(t.getId(), req.getCOHFCircuitsesIds(), req.getCOHFCircuitsesNames());
		saveEbbMlls(t.getId(), req.getCEBBMLLsIds(), req.getCEBBMLLsNames());
		linkAttachmentsToTask(t.getId(), req.getAttachmentsIds());
		logCreate(t.getId(), req);

		String userMsg = buildCreateMessage(t, req);
		String groupMsg = buildGroupStatusMessage("📌 New Task", t.getCSRNumber(), t.getParentName(), t.getStatus(),
				null);
		notifyAllUsers(req.getAssignedUserId(), req.getCSecondaryAssignedUserIds(), userMsg);
		notifyAccountGroup(t.getAccountId(), groupMsg);
		notifyAdminGroup(userMsg); // ← ADD

		return toResponseFromDb(t);
	}

	// ── GET ───────────────────────────────────────────────────────────────────
	public TaskResponse get(String id) {
		Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found: " + id));
		return toResponseFromDb(t);
	}

	// ── LIST ──────────────────────────────────────────────────────────────────
	public List<TaskResponse> list(String status, String assignedUserId, String accountId) {
		List<Task> all;
		if (status != null)
			all = taskRepo.findByStatus(status);
		else if (assignedUserId != null)
			all = taskRepo.findByAssignedUserId(assignedUserId);
		else if (accountId != null)
			all = taskRepo.findByAccountId(accountId);
		else
			all = taskRepo.findAll();

		return all.stream().map(t -> {
			try {
				return toResponseFromDb(t);
			} catch (Exception e) {
				log.error("Error mapping task {}: {}", t.getId(), e.getMessage());
				return baseFields(t);
			}
		}).collect(Collectors.toList());
	}

	// ── UPDATE ────────────────────────────────────────────────────────────────
	@Transactional
	public TaskResponse update(String id, TaskRequest req) {
		Task old = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found: " + id));

		String oldStatus = old.getStatus();
		boolean wasCompleted = "Completed".equals(oldStatus);
		boolean isCompleting = "Completed".equals(req.getStatus()) && !wasCompleted;

		if (isCompleting)
			validateMaterialOnComplete(old, req);

		// Auto-set dateCompleted before detectChanges so it flows through map() and
		// audit log
		if (isCompleting && req.getDateCompleted() == null) {
			req.setDateCompleted(LocalDateTime.now());
		}

		// Clear dateCompleted + duration if re-opening a completed task
		if (wasCompleted && req.getStatus() != null && !"Completed".equals(req.getStatus())) {
			req.setDateCompleted(null);
			old.setDateCompleted(null);
			old.setCDurationText(null);
		}

		Map<String, Boolean> changed = detectChanges(old, req);
		map(old, req);

		// Auto-calculate duration = dateCompleted − acceptedAt (fallback to dateStart)
		if (isCompleting) {
			LocalDateTime completedAt = old.getDateCompleted();
			LocalDateTime acceptedAt = old.getAcceptedAt();
			LocalDateTime from = (acceptedAt != null) ? acceptedAt : old.getDateStart();
			if (completedAt != null && from != null) {
				long diffMins = java.time.Duration.between(from, completedAt).toMinutes();
				if (diffMins >= 0) {
					long days = diffMins / 1440;
					long hours = (diffMins % 1440) / 60;
					long mins = diffMins % 60;
					StringBuilder dur = new StringBuilder();
					if (days > 0)
						dur.append(days).append("d ");
					if (hours > 0)
						dur.append(hours).append("h ");
					if (mins > 0)
						dur.append(mins).append("m");
					old.setCDurationText(dur.toString().trim().isEmpty() ? "0m" : dur.toString().trim());
				}
			}
		}

		resolveAccount(old, req.getParentId() != null ? req.getParentId() : old.getParentId(),
				req.getParentType() != null ? req.getParentType() : old.getParentType(),
				req.getDescription() != null ? req.getDescription() : old.getDescription());

		old.setModifiedAt(LocalDateTime.now());
		taskRepo.save(old);

		if (req.getCOHFCircuitsesIds() != null) {
			taskCircuitRepo.deleteByTaskId(id);
			saveCircuits(id, req.getCOHFCircuitsesIds(), req.getCOHFCircuitsesNames());
		}
		if (req.getCEBBMLLsIds() != null) {
			taskEbbMllRepo.deleteByTaskId(id);
			saveEbbMlls(id, req.getCEBBMLLsIds(), req.getCEBBMLLsNames());
		}
		if (req.getAttachmentsIds() != null) {
			reconcileAttachments(id, req.getAttachmentsIds());
		}

		String requestedStatus = req.getStatus();
		boolean statusActuallyChanged = requestedStatus != null && !requestedStatus.equals(oldStatus);

		if (statusActuallyChanged) {
			addSystemComment(old, "status", Map.of("from", oldStatus != null ? oldStatus : "", "to", requestedStatus),
					currentUserId(), currentUserName());
		}

		List<String> changedFieldNames = changed.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey)
				.collect(Collectors.toList());
		if (!changedFieldNames.isEmpty()) {
			addSystemComment(old, "update", changed, currentUserId(), currentUserName());
		}

		if (isCompleting)
			deductInventoryOnComplete(old, id);

		if (statusActuallyChanged || !changedFieldNames.isEmpty()) {
			List<String> secondaryIds = resolveSecondaryIds(old, req);
			String statusForMsg = statusActuallyChanged ? requestedStatus : null;

			// Full detail message → assigned users only
			String userMsg2 = buildUpdateMessage(old, req, statusForMsg, changedFieldNames);
			notifyAllUsers(old.getAssignedUserId(), secondaryIds, userMsg2);
			notifyAdminGroup(userMsg2); // ← ADD

			// Compact status-only message → account group chat
			if (statusActuallyChanged) {
				String groupMsg2 = buildGroupStatusMessage("📌 Task Update", old.getCSRNumber(), old.getParentName(),
						requestedStatus, null);
				notifyAccountGroup(old.getAccountId(), groupMsg2);
			}
			// If only field changes (no status change), don't notify account group at all
		}

		return toResponseFromDb(old);
	}

	// ── DELETE ────────────────────────────────────────────────────────────────
	@Transactional
	public void delete(String id) {
		if (!taskRepo.existsById(id))
			throw new RuntimeException("Task not found: " + id);

		taskCircuitRepo.deleteByTaskId(id);
		taskEbbMllRepo.deleteByTaskId(id);
		taskCommentRepo.findByTaskIdOrderByCreatedAtAsc(id).forEach(c -> taskCommentRepo.deleteById(c.getId()));

		List<Attachment> linked = attachmentRepo.findByRelatedIdAndRelatedType(id, "Task");
		for (Attachment a : linked) {
			deleteAttachmentFile(a);
			attachmentRepo.deleteById(a.getId());
		}

		taskRepo.deleteById(id);
	}

	// ── COMMENT ───────────────────────────────────────────────────────────────
	@Transactional
	public TaskCommentDto addComment(String taskId, TaskCommentRequest req) {
		if (!taskRepo.existsById(taskId))
			throw new RuntimeException("Task not found: " + taskId);

		TaskComment c = new TaskComment();
		c.setId(UUID.randomUUID().toString());
		c.setTaskId(taskId);
		c.setUserId(req.getUserId());
		c.setUserName(req.getUserName());
		c.setAvatarId(req.getAvatarId());
		c.setText(req.getText());
		c.setType("comment");
		c.setCreatedAt(LocalDateTime.now());
		taskCommentRepo.save(c);
		return toCommentDto(c);
	}

	// ── ATTACHMENT HELPERS ────────────────────────────────────────────────────

	private void linkAttachmentsToTask(String taskId, List<String> ids) {
		if (ids == null || ids.isEmpty())
			return;
		for (String attId : ids) {
			if (attId == null || attId.isBlank())
				continue;
			attachmentRepo.findById(attId).ifPresent(a -> {
				if ("temp".equals(a.getRelatedId()) || a.getRelatedId() == null) {
					a.setRelatedId(taskId);
					a.setRelatedType("Task");
					attachmentRepo.save(a);
				}
			});
		}
	}

	private void reconcileAttachments(String taskId, List<String> requestedIds) {
		List<Attachment> current = attachmentRepo.findByRelatedIdAndRelatedType(taskId, "Task");
		Set<String> keep = new HashSet<>(requestedIds);
		for (Attachment a : current) {
			if (!keep.contains(a.getId())) {
				deleteAttachmentFile(a);
				attachmentRepo.deleteById(a.getId());
				log.info("Removed attachment {} from task {}", a.getId(), taskId);
			}
		}
		linkAttachmentsToTask(taskId, requestedIds);
	}

	private void deleteAttachmentFile(Attachment a) {
		if (a.getPath() == null || a.getPath().isBlank())
			return;
		try {
			Files.deleteIfExists(Paths.get(a.getPath()));
		} catch (Exception ex) {
			log.warn("Could not delete attachment file {}: {}", a.getPath(), ex.getMessage());
		}
	}

	// ── INVENTORY DEDUCTION ───────────────────────────────────────────────────

	private void deductInventoryOnComplete(Task task, String taskId) {
		try {
			String storeAreaCode = resolveStoreAreaCode(task.getAssignedUserId());
			if (storeAreaCode == null) {
				log.warn("Task {} completed — no team/store for user {} — inventory skipped", taskId,
						task.getAssignedUserId());
				return;
			}
			String taskName = task.getName();
			String userId = currentUserId();
			String userName = currentUserName();

			Double fiberUsed = task.getFiberUsedMtr();
			if (fiberUsed != null && fiberUsed > 0 && task.getOfcType() != null) {
				deductStock(resolveOfcItemId(task.getOfcType()), resolveOfcItemName(task.getOfcType()), storeAreaCode,
						fiberUsed, taskId, taskName, userId, userName);
			}
			Integer medium = task.getMediumJcBoxUsed();
			if (medium != null && medium > 0) {
				deductStock(JCV_MEDIUM, "JCV Box - Medium", storeAreaCode, medium.doubleValue(), taskId, taskName,
						userId, userName);
			}
			Integer small = task.getSmallJcBoxUsed();
			if (small != null && small > 0) {
				deductStock(JCV_SMALL, "JCV Box - Small", storeAreaCode, small.doubleValue(), taskId, taskName, userId,
						userName);
			}
		} catch (Exception e) {
			log.error("Inventory deduction failed for task {}: {}", taskId, e.getMessage(), e);
		}
	}

	private void deductStock(String itemId, String itemName, String storeAreaCode, double qty, String taskId,
			String taskName, String performedById, String performedByName) {

		InventoryStock stock = inventoryStockRepo.findByItemIdAndStoreAreaCode(itemId, storeAreaCode).orElseGet(() -> {
			log.warn("No stock record for item {} in store {} — creating at 0", itemName, storeAreaCode);
			InventoryStock s = new InventoryStock();
			s.setId(UUID.randomUUID().toString());
			s.setItemId(itemId);
			s.setStoreAreaCode(storeAreaCode);
			s.setQuantityOnHand(0.0);
			return inventoryStockRepo.save(s);
		});

		double before = stock.getQuantityOnHand();
		double after = Math.max(0, before - qty);
		if (before < qty) {
			log.warn("Insufficient stock: {} in store {} — available: {}, needed: {}", itemName, storeAreaCode, before,
					qty);
		}
		stock.setQuantityOnHand(after);
		inventoryStockRepo.save(stock);

		InventoryTransaction txn = new InventoryTransaction();
		txn.setId(UUID.randomUUID().toString());
		txn.setTxnType("USAGE");
		txn.setItemId(itemId);
		txn.setItemName(itemName);
		txn.setStoreAreaCode(storeAreaCode);
		txn.setStoreName(stock.getStoreName());
		txn.setQuantityChange(-qty);
		txn.setQuantityAfter(after);
		txn.setTaskId(taskId);
		txn.setTaskName(taskName);
		txn.setPerformedById(performedById);
		txn.setPerformedByName(performedByName);
		txn.setNotes("Auto-deducted on task completion");
		inventoryTxnRepo.save(txn);
	}

	private String resolveStoreAreaCode(String userId) {
		if (userId == null)
			return null;
		for (UserTeam ut : userTeamRepo.findByUserId(userId)) {
			Optional<Team> team = teamRepo.findById(ut.getTeamId());
			if (team.isPresent() && team.get().getAreaCode() != null && !team.get().getAreaCode().isBlank()) {
				return team.get().getAreaCode();
			}
		}
		return null;
	}

	private String resolveOfcItemId(String ofcType) {
		if (ofcType == null)
			return "ofc-6f";
		return switch (ofcType.toLowerCase().replaceAll("[^0-9f]", "")) {
		case "4f" -> "ofc-4f";
		case "12f" -> "ofc-12f";
		default -> "ofc-6f";
		};
	}

	private String resolveOfcItemName(String ofcType) {
		if (ofcType == null)
			return "OFC Cable";
		return switch (ofcType.toLowerCase().replaceAll("[^0-9f]", "")) {
		case "4f" -> "OFC Cable - 4F";
		case "12f" -> "OFC Cable - 12F";
		default -> "OFC Cable - 6F";
		};
	}

	// ── TELEGRAM HELPERS ──────────────────────────────────────────────────────

	private void notifyAllUsers(String assignedUserId, List<String> secondaryIds, String message) {
		notifyUser(assignedUserId, message);
		if (secondaryIds != null) {
			secondaryIds.stream().filter(id -> id != null && !id.isBlank()).forEach(id -> notifyUser(id, message));
		}
	}

	private void notifyUser(String userId, String message) {
		if (userId == null || userId.isBlank())
			return;
		userRepo.findById(userId).ifPresent(u -> {
			String chatId = u.getTelegramUsername();
			if (chatId != null && !chatId.isBlank()) {
				log.info("Notifying user userId={} chatId={}", userId, chatId);
				telegramService.sendMessage(chatId, message);
			} else {
				log.warn("User {} has no Telegram ID configured", userId);
			}
		});
	}

	private void notifyAccountGroup(String accountId, String message) {
		if (accountId == null || accountId.isBlank())
			return;
		accountRepo.findById(accountId).ifPresent(acc -> {
			String groupChatId = acc.getCTelegramgroupchatid();
			if (groupChatId != null && !groupChatId.isBlank()) {
				log.info("Notifying account group accountId={} chatId={}", accountId, groupChatId);
				telegramService.sendMessage(groupChatId, message);
			} else {
				log.warn("Account {} has no Telegram group chat ID configured", accountId);
			}
		});
	}

	// ── MESSAGE BUILDERS ──────────────────────────────────────────────────────

	private String buildCreateMessage(Task t, TaskRequest req) {
		return buildStandardMessage("📌 New Task Created", t, req, null);
	}

	private String buildUpdateMessage(Task t, TaskRequest req, String newStatus, List<String> changedFields) {
		String header = (newStatus != null && !newStatus.isBlank()) ? "📌 Task Status Updated" : "📌 Task Updated";
		StringBuilder footer = new StringBuilder();
		if (newStatus != null && !newStatus.isBlank())
			footer.append("\n<b>🔔 Status changed to:</b> ").append(newStatus);
		if (changedFields != null && !changedFields.isEmpty())
			footer.append("\n<b>Updated Fields:</b> ").append(String.join(", ", changedFields));
		return buildStandardMessage(header, t, req, footer.length() > 0 ? footer.toString() : null);
	}

	private String buildStandardMessage(String header, Task t, TaskRequest req, String footer) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b>").append(header).append("</b>\n\n");
		String circuitName = coalesce(req != null ? req.getParentName() : null, t.getParentName());
		sb.append("<b>Circuit Name:</b> ").append(coalesce(circuitName, "—")).append("\n");
		sb.append("<b>Account:</b> ").append(coalesce(t.getAccountName(), "—")).append("\n");
		sb.append("<b>SR Number:</b> ").append(coalesce(t.getCSRNumber(), "—")).append("\n");
		sb.append("<b>Status:</b> ").append(coalesce(t.getStatus(), "New")).append("\n");
		sb.append("<b>Priority:</b> ").append(coalesce(t.getPriority(), "Normal")).append("\n");
		sb.append("<b>Work Type:</b> ").append(coalesce(t.getCWorkType(), "—")).append("\n");
		String circuitId = extractCircuitId(
				req != null && req.getDescription() != null ? req.getDescription() : t.getDescription());
		if (circuitId != null && !circuitId.isBlank())
			sb.append("<b>Circuit ID:</b> ").append(circuitId).append("\n");
		String assignedName = coalesce(req != null ? req.getAssignedUserName() : null, t.getAssignedUserName());
		if (!isBlank(assignedName))
			sb.append("<b>Assigned User:</b> ").append(assignedName).append("\n");
		String secondaryNames = resolveSecondaryNamesString(t, req);
		if (!isBlank(secondaryNames))
			sb.append("<b>Secondary Assigned User:</b> ").append(secondaryNames).append("\n");
		String note = req != null && req.getCNote() != null ? req.getCNote() : t.getCNote();
		if (!isBlank(note))
			sb.append("<b>Note:</b> ").append(note).append("\n");
		String description = req != null && req.getDescription() != null ? req.getDescription() : t.getDescription();
		if (!isBlank(description)) {
			sb.append("<b>Description:</b> ").append(description);
			if (!description.endsWith("\n"))
				sb.append("\n");
		}
		String materialBlock = buildMaterialUsedBlock(t, req);
		if (!isBlank(materialBlock))
			sb.append("\n").append(materialBlock);
		if (!isBlank(footer))
			sb.append(footer);
		return sb.toString();
	}

	private String buildMaterialUsedBlock(Task t, TaskRequest req) {
		String ofcType = pickStr(req != null ? req.getOfcType() : null, t.getOfcType());
		Double ofcStart = pickDbl(req != null ? req.getOfcStartingMtr() : null, t.getOfcStartingMtr());
		Double ofcEnd = pickDbl(req != null ? req.getOfcEndingMtr() : null, t.getOfcEndingMtr());
		Double fiberUsed = pickDbl(req != null ? req.getFiberUsedMtr() : null, t.getFiberUsedMtr());
		Integer mediumBox = pickInt(req != null ? req.getMediumJcBoxUsed() : null, t.getMediumJcBoxUsed());
		Integer smallBox = pickInt(req != null ? req.getSmallJcBoxUsed() : null, t.getSmallJcBoxUsed());
		Integer patchCable = pickInt(req != null ? req.getPatchCableUsed() : null, t.getPatchCableUsed());
		boolean any = !isBlank(ofcType) || ofcStart != null || ofcEnd != null || fiberUsed != null || mediumBox != null
				|| smallBox != null || patchCable != null;
		if (!any)
			return null;
		if (fiberUsed == null && ofcStart != null && ofcEnd != null)
			fiberUsed = Math.abs(ofcEnd - ofcStart);
		StringBuilder b = new StringBuilder();
		b.append("<b>📦 Material Used</b>\n");
		if (!isBlank(ofcType))
			b.append("<b>OFC Type:</b> ").append(ofcType).append("\n");
		if (ofcStart != null)
			b.append("<b>OFC Starting (m):</b> ").append(fmtNum(ofcStart)).append("\n");
		if (ofcEnd != null)
			b.append("<b>OFC Ending (m):</b> ").append(fmtNum(ofcEnd)).append("\n");
		if (fiberUsed != null)
			b.append("<b>OFC Cable Used (m):</b> ").append(fmtNum(fiberUsed)).append("\n");
		if (mediumBox != null)
			b.append("<b>Medium JC Box Used:</b> ").append(mediumBox).append("\n");
		if (smallBox != null)
			b.append("<b>Small JC Box Used:</b> ").append(smallBox).append("\n");
		if (patchCable != null)
			b.append("<b>Patch Cable Used:</b> ").append(patchCable).append("\n");
		return b.toString();
	}

	private String extractCircuitId(String description) {
		if (isBlank(description))
			return null;
		for (String line : description.split("\\R")) {
			String t = line.trim();
			if (t.regionMatches(true, 0, "Circuit ID:", 0, 11)) {
				String val = t.substring(11).trim();
				return val.isEmpty() ? null : val;
			}
		}
		return null;
	}

	private String resolveSecondaryNamesString(Task t, TaskRequest req) {
		if (req != null && req.getCSecondaryAssignedUserIds() != null && req.getCSecondaryAssignedUserNames() != null) {
			String joined = req.getCSecondaryAssignedUserIds().stream()
					.map(id -> req.getCSecondaryAssignedUserNames().getOrDefault(id, "")).filter(n -> !isBlank(n))
					.collect(Collectors.joining(", "));
			if (!joined.isBlank())
				return joined;
		}
		if (!isBlank(t.getCSecondaryUserNames())) {
			return Arrays.stream(t.getCSecondaryUserNames().split(",")).map(String::trim).filter(n -> !n.isBlank())
					.collect(Collectors.joining(", "));
		}
		return !isBlank(t.getCSecondaryAssignedUserName()) ? t.getCSecondaryAssignedUserName() : null;
	}

	// ── ACTIVITY LOG ─────────────────────────────────────────────────────────

	private void addSystemComment(Task task, String type, Object data, String userId, String userName) {
		try {
			TaskComment c = new TaskComment();
			c.setId(UUID.randomUUID().toString());
			c.setTaskId(task.getId());
			c.setType(type);
			c.setUserId(userId);
			c.setUserName(userName);
			c.setCreatedAt(LocalDateTime.now());
			if (data != null)
				c.setData(objectMapper.writeValueAsString(data));
			userRepo.findById(userId != null ? userId : "").ifPresent(u -> c.setAvatarId(u.getAvatarId()));
			taskCommentRepo.save(c);
		} catch (Exception ignored) {
		}
	}

	// ── CHANGE DETECTION ─────────────────────────────────────────────────────

	private Map<String, Boolean> detectChanges(Task old, TaskRequest req) {
		Map<String, Boolean> m = new LinkedHashMap<>();
		m.put("RFO", changed(old.getCRFO(), req.getCRFO()));
		m.put("Fiber Used (Mtr)", changed(old.getFiberUsedMtr(), req.getFiberUsedMtr()));
		m.put("OFC Type", changed(old.getOfcType(), req.getOfcType()));
		m.put("OFC Starting Mtr", changed(old.getOfcStartingMtr(), req.getOfcStartingMtr()));
		m.put("OFC Ending Mtr", changed(old.getOfcEndingMtr(), req.getOfcEndingMtr()));
		m.put("Medium JC Box Used (No)", changed(old.getMediumJcBoxUsed(), req.getMediumJcBoxUsed()));
		m.put("Small JC Box Used (No)", changed(old.getSmallJcBoxUsed(), req.getSmallJcBoxUsed()));
		m.put("Patch Cable Used (No)", changed(old.getPatchCableUsed(), req.getPatchCableUsed()));
		m.put("Description", changed(old.getDescription(), req.getDescription()));
		m.put("Note", changed(old.getCNote(), req.getCNote()));
		m.put("Assigned User", changed(old.getAssignedUserId(), req.getAssignedUserId()));
		m.put("Date Completed", changed(old.getDateCompleted(), req.getDateCompleted()));
		return m;
	}

	private boolean changed(Object o, Object n) {
		return n != null && !n.equals(o);
	}

	// ── VALIDATION ───────────────────────────────────────────────────────────

	private void validateMaterialOnComplete(Task current, TaskRequest req) {
		String workType = req.getCWorkType() != null ? req.getCWorkType() : current.getCWorkType();
		if ("Survey".equalsIgnoreCase(workType))
			return;
		String ofcType = req.getOfcType() != null ? req.getOfcType() : current.getOfcType();
		Double ofcStart = req.getOfcStartingMtr() != null ? req.getOfcStartingMtr() : current.getOfcStartingMtr();
		Double ofcEnd = req.getOfcEndingMtr() != null ? req.getOfcEndingMtr() : current.getOfcEndingMtr();
		Integer mediumBox = req.getMediumJcBoxUsed() != null ? req.getMediumJcBoxUsed() : current.getMediumJcBoxUsed();
		Integer smallBox = req.getSmallJcBoxUsed() != null ? req.getSmallJcBoxUsed() : current.getSmallJcBoxUsed();
		Integer patchCable = req.getPatchCableUsed() != null ? req.getPatchCableUsed() : current.getPatchCableUsed();
		StringBuilder missing = new StringBuilder();
		if (isBlank(ofcType))
			missing.append("OFC Type, ");
		if (ofcStart == null)
			missing.append("OFC Starting Mtr, ");
		if (ofcEnd == null)
			missing.append("OFC Ending Mtr, ");
		if (mediumBox == null)
			missing.append("Medium JC Box Used, ");
		if (smallBox == null)
			missing.append("Small JC Box Used, ");
		if (patchCable == null)
			missing.append("Patch Cable Used, ");
		if (missing.length() > 0)
			throw new RuntimeException("Cannot complete task: Material Used fields are required — "
					+ missing.substring(0, missing.length() - 2));
	}

	// ── MAPPING ───────────────────────────────────────────────────────────────

	private void map(Task t, TaskRequest r) {
		if (r.getName() != null)
			t.setName(r.getName());
		if (r.getStatus() != null)
			t.setStatus(r.getStatus());
		else if (t.getStatus() == null)
			t.setStatus("New");
		if (r.getPriority() != null)
			t.setPriority(r.getPriority());
		else if (t.getPriority() == null)
			t.setPriority("Normal");
		if (r.getCWorkType() != null)
			t.setCWorkType(r.getCWorkType());
		if (r.getCRFO() != null)
			t.setCRFO(r.getCRFO());
		if (r.getParentId() != null)
			t.setParentId(r.getParentId());
		if (r.getParentType() != null)
			t.setParentType(r.getParentType());
		if (r.getParentName() != null)
			t.setParentName(r.getParentName());
		// dateStart NOT mapped — immutable, set once at creation
		// cDurationText NOT mapped — computed by backend on completion
		if (r.getDateCompleted() != null)
			t.setDateCompleted(r.getDateCompleted());
		if (r.getDescription() != null)
			t.setDescription(r.getDescription());
		if (r.getCNote() != null)
			t.setCNote(r.getCNote());
		if (r.getAssignedUserId() != null)
			t.setAssignedUserId(r.getAssignedUserId());
		if (r.getAssignedUserName() != null)
			t.setAssignedUserName(r.getAssignedUserName());
		if (r.getOfcType() != null)
			t.setOfcType(r.getOfcType());
		if (r.getOfcStartingMtr() != null)
			t.setOfcStartingMtr(r.getOfcStartingMtr());
		if (r.getOfcEndingMtr() != null)
			t.setOfcEndingMtr(r.getOfcEndingMtr());
		if (r.getFiberUsedMtr() != null)
			t.setFiberUsedMtr(r.getFiberUsedMtr());
		if (r.getMediumJcBoxUsed() != null)
			t.setMediumJcBoxUsed(r.getMediumJcBoxUsed());
		if (r.getSmallJcBoxUsed() != null)
			t.setSmallJcBoxUsed(r.getSmallJcBoxUsed());
		if (r.getPatchCableUsed() != null)
			t.setPatchCableUsed(r.getPatchCableUsed());
		if (r.getCFieldNotes() != null)
			t.setCFieldNotes(r.getCFieldNotes());
		if (r.getCResolutionNotes() != null)
			t.setCResolutionNotes(r.getCResolutionNotes());
		if (r.getAcceptanceTimeMins() != null)
			t.setAcceptanceTimeMins(r.getAcceptanceTimeMins());
		if (r.getCSecondaryAssignedUserIds() != null) {
			t.setCSecondaryUserIds(String.join(",", r.getCSecondaryAssignedUserIds()));
		}
		if (r.getCSecondaryAssignedUserNames() != null && !r.getCSecondaryAssignedUserNames().isEmpty()
				&& r.getCSecondaryAssignedUserIds() != null) {
			String names = r.getCSecondaryAssignedUserIds().stream()
					.map(id -> r.getCSecondaryAssignedUserNames().getOrDefault(id, ""))
					.collect(Collectors.joining(","));
			t.setCSecondaryUserNames(names);
		}
	}

	private void resolveAccount(Task t, String parentId, String parentType, String description) {
		if (parentId == null || parentId.isBlank())
			return;
		if ("EBBMLLs".equals(parentType)) {
			ebbMllRepo.findById(parentId).ifPresent(e -> {
				if (e.getAccountId() != null) {
					t.setAccountId(e.getAccountId());
					t.setAccountName(e.getAccountName());
				}
			});
		} else {
			circuitRepo.findById(parentId).ifPresent(c -> {
				if (c.getAccountId() != null) {
					t.setAccountId(c.getAccountId());
					t.setAccountName(c.getAccountName());
				}
			});
		}
	}

	private void logCreate(String taskId, TaskRequest req) {
		String actorId = currentUserId();
		String actorName = currentUserName();
		String assignedName = req.getAssignedUserName() != null ? req.getAssignedUserName() : "Unassigned";
		TaskComment c = new TaskComment();
		c.setId(UUID.randomUUID().toString());
		c.setTaskId(taskId);
		c.setUserId(actorId);
		c.setUserName(actorName);
		c.setType("create");
		c.setData("{\"assignedTo\":\"" + esc(assignedName) + "\"," + "\"status\":\""
				+ esc(coalesce(req.getStatus(), "New")) + "\"}");
		c.setCreatedAt(LocalDateTime.now());
		taskCommentRepo.save(c);
	}

	private void logStatus(String taskId, TaskRequest req, String newStatus) {
		TaskComment c = buildLog(taskId, currentUserId(), currentUserName(), "status",
				"{\"status\":\"" + esc(newStatus) + "\"}");
		taskCommentRepo.save(c);
	}

	public List<TaskResponse> listForUser(String userId) {
		return taskRepo.findByAssignedUserIdOrSecondary(userId).stream().map(t -> {
			try {
				return toResponseFromDb(t);
			} catch (Exception e) {
				log.error("Error mapping task {}: {}", t.getId(), e.getMessage());
				return baseFields(t);
			}
		}).collect(Collectors.toList());
	}

	private void logUpdate(String taskId, TaskRequest req, List<String> fields) {
		String fieldsJson = fields.stream().map(f -> "\"" + esc(f) + "\"").collect(Collectors.joining(",", "[", "]"));
		TaskComment c = buildLog(taskId, currentUserId(), currentUserName(), "update",
				"{\"fields\":" + fieldsJson + "}");
		taskCommentRepo.save(c);
	}

	private TaskComment buildLog(String taskId, String userId, String userName, String type, String data) {
		TaskComment c = new TaskComment();
		c.setId(UUID.randomUUID().toString());
		c.setTaskId(taskId);
		c.setUserId(userId);
		c.setUserName(userName);
		c.setType(type);
		c.setData(data);
		c.setCreatedAt(LocalDateTime.now());
		return c;
	}

	private List<String> resolveSecondaryIds(Task saved, TaskRequest req) {
		if (req.getCSecondaryAssignedUserIds() != null)
			return req.getCSecondaryAssignedUserIds();
		if (!isBlank(saved.getCSecondaryUserIds()))
			return Arrays.asList(saved.getCSecondaryUserIds().split(","));
		if (saved.getCSecondaryAssignedUserId() != null)
			return List.of(saved.getCSecondaryAssignedUserId());
		return List.of();
	}

	private String generateSRNumber() {
		long count = taskRepo.count() + 1;
		return "SR" + String.format("%04d", count);
	}

	private void saveCircuits(String taskId, List<String> ids, Map<String, String> names) {
		if (ids == null || ids.isEmpty())
			return;
		Map<String, String> seen = new LinkedHashMap<>();
		for (String cid : ids)
			seen.putIfAbsent(cid, names != null ? names.getOrDefault(cid, "") : "");
		seen.forEach((cid, cname) -> taskCircuitRepo.save(new TaskCircuit(null, taskId, cid, cname)));
	}

	private void saveEbbMlls(String taskId, List<String> ids, Map<String, String> names) {
		if (ids == null || ids.isEmpty())
			return;
		Map<String, String> seen = new LinkedHashMap<>();
		for (String eid : ids)
			seen.putIfAbsent(eid, names != null ? names.getOrDefault(eid, "") : "");
		seen.forEach((eid, ename) -> taskEbbMllRepo.save(new TaskEbbMll(null, taskId, eid, ename)));
	}

	// ── RESPONSE BUILDERS ─────────────────────────────────────────────────────

	private TaskResponse toResponseFromDb(Task t) {
		TaskResponse res = baseFields(t);
		List<TaskCircuit> circuits = taskCircuitRepo.findByTaskId(t.getId());
		res.setCOHFCircuitsesIds(circuits.stream().map(TaskCircuit::getCircuitId).filter(Objects::nonNull).distinct()
				.collect(Collectors.toList()));
		Map<String, String> circuitNames = new LinkedHashMap<>();
		for (TaskCircuit c : circuits) {
			if (c.getCircuitId() == null)
				continue;
			String name = c.getCircuitName();
			if (isBlank(name))
				name = circuitRepo.findById(c.getCircuitId()).map(OhfCircuit::getName).orElse(c.getCircuitId());
			circuitNames.putIfAbsent(c.getCircuitId(), name);
		}
		res.setCOHFCircuitsesNames(circuitNames);
		List<TaskEbbMll> ebbMlls = taskEbbMllRepo.findByTaskId(t.getId());
		res.setCEBBMLLsIds(ebbMlls.stream().map(TaskEbbMll::getEbbMllId).filter(Objects::nonNull).distinct()
				.collect(Collectors.toList()));
		Map<String, String> ebbNames = new LinkedHashMap<>();
		for (TaskEbbMll e : ebbMlls) {
			if (e.getEbbMllId() == null)
				continue;
			String name = e.getEbbMllName();
			if (isBlank(name))
				name = ebbMllRepo.findById(e.getEbbMllId()).map(EbbMll::getName).orElse(e.getEbbMllId());
			ebbNames.putIfAbsent(e.getEbbMllId(), name);
		}
		res.setCEBBMLLsNames(ebbNames);
		List<Attachment> atts = attachmentRepo.findByRelatedIdAndRelatedType(t.getId(), "Task");
		res.setAttachmentsIds(
				atts.stream().map(Attachment::getId).filter(Objects::nonNull).collect(Collectors.toList()));
		Map<String, String> attNames = new LinkedHashMap<>();
		Map<String, String> attTypes = new LinkedHashMap<>();
		for (Attachment a : atts) {
			if (a.getId() == null)
				continue;
			if (a.getName() != null)
				attNames.put(a.getId(), a.getName());
			if (a.getType() != null)
				attTypes.put(a.getId(), a.getType());
		}
		res.setAttachmentsNames(attNames);
		res.setAttachmentsTypes(attTypes);
		res.setStream(taskCommentRepo.findByTaskIdOrderByCreatedAtAsc(t.getId()).stream().map(this::toCommentDto)
				.collect(Collectors.toList()));
		return res;
	}

	private TaskResponse baseFields(Task t) {
		TaskResponse res = new TaskResponse();
		res.setId(t.getId());
		res.setName(t.getName());
		res.setStatus(t.getStatus());
		res.setPriority(t.getPriority());
		res.setCWorkType(t.getCWorkType());
		res.setCRFO(t.getCRFO());
		res.setCSRNumber(t.getCSRNumber());
		res.setParentId(t.getParentId());
		res.setParentType(t.getParentType());
		res.setParentName(t.getParentName());
		res.setDateStart(t.getDateStart());
		res.setDateStartDate(t.getDateStartDate());
		res.setDateCompleted(t.getDateCompleted());
		res.setCDurationText(t.getCDurationText());
		res.setDescription(t.getDescription());
		res.setCNote(t.getCNote());
		res.setAssignedUserId(t.getAssignedUserId());
		res.setAssignedUserName(t.getAssignedUserName());
		res.setAccountId(t.getAccountId());
		res.setAccountName(t.getAccountName());
		res.setOfcType(t.getOfcType());
		res.setOfcStartingMtr(t.getOfcStartingMtr());
		res.setOfcEndingMtr(t.getOfcEndingMtr());
		res.setFiberUsedMtr(t.getFiberUsedMtr());
		res.setMediumJcBoxUsed(t.getMediumJcBoxUsed());
		res.setSmallJcBoxUsed(t.getSmallJcBoxUsed());
		res.setPatchCableUsed(t.getPatchCableUsed());
		res.setCFieldNotes(t.getCFieldNotes());
		res.setCResolutionNotes(t.getCResolutionNotes());
		res.setCreatedAt(t.getCreatedAt());
		res.setModifiedAt(t.getModifiedAt());
		res.setCreatedById(t.getCreatedById());
		res.setCreatedByName(t.getCreatedByName());
		res.setAcceptanceTimeMins(t.getAcceptanceTimeMins());
		res.setAcceptedAt(t.getAcceptedAt());
		res.setAcceptedById(t.getAcceptedById());
		res.setAcceptedByName(t.getAcceptedByName());
		if (!isBlank(t.getCSecondaryUserIds())) {
			List<String> ids = Arrays.asList(t.getCSecondaryUserIds().split(","));
			res.setCSecondaryAssignedUserIds(ids);
			if (t.getCSecondaryUserNames() != null) {
				String[] names = t.getCSecondaryUserNames().split(",", -1);
				Map<String, String> map = new LinkedHashMap<>();
				for (int i = 0; i < ids.size(); i++)
					map.put(ids.get(i), i < names.length ? names[i] : ids.get(i));
				res.setCSecondaryAssignedUserNames(map);
			} else {
				res.setCSecondaryAssignedUserNames(Map.of());
			}
		} else if (t.getCSecondaryAssignedUserId() != null) {
			res.setCSecondaryAssignedUserIds(List.of(t.getCSecondaryAssignedUserId()));
			res.setCSecondaryAssignedUserNames(Map.of(t.getCSecondaryAssignedUserId(),
					coalesce(t.getCSecondaryAssignedUserName(), t.getCSecondaryAssignedUserId())));
		} else {
			res.setCSecondaryAssignedUserIds(List.of());
			res.setCSecondaryAssignedUserNames(Map.of());
		}
		res.setCOHFCircuitsesIds(List.of());
		res.setCOHFCircuitsesNames(Map.of());
		res.setCEBBMLLsIds(List.of());
		res.setCEBBMLLsNames(Map.of());
		res.setAttachmentsIds(List.of());
		res.setAttachmentsNames(Map.of());
		res.setAttachmentsTypes(Map.of());
		res.setStream(List.of());
		return res;
	}

	private TaskCommentDto toCommentDto(TaskComment c) {
		TaskCommentDto dto = new TaskCommentDto();
		dto.setId(c.getId());
		dto.setUserId(c.getUserId());
		dto.setUserName(c.getUserName());
		dto.setAvatarId(c.getAvatarId());
		dto.setText(c.getText());
		dto.setType(c.getType());
		dto.setData(c.getData());
		dto.setCreatedAt(c.getCreatedAt());
		return dto;
	}

	// ── UTILS ─────────────────────────────────────────────────────────────────

	private String currentUserId() {
		try {
			var auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.getPrincipal() instanceof String uid && !uid.isBlank())
				return uid;
		} catch (Exception ignored) {
		}
		return null;
	}

	private String currentUserName() {
		try {
			String uid = currentUserId();
			if (uid == null)
				return null;
			return userRepo.findById(uid).map(u -> u.getName() != null ? u.getName() : u.getUserName()).orElse(null);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static String coalesce(String a, String b) {
		return a != null ? a : b;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("\"", "\\\"");
	}

	private static String pickStr(String req, String db) {
		return !isBlank(req) ? req : db;
	}

	private static Double pickDbl(Double req, Double db) {
		return req != null ? req : db;
	}

	private static Integer pickInt(Integer req, Integer db) {
		return req != null ? req : db;
	}

	private static String fmtNum(Double d) {
		if (d == null)
			return "—";
		return (d == Math.floor(d) && !Double.isInfinite(d)) ? String.valueOf(d.longValue()) : String.valueOf(d);
	}
}