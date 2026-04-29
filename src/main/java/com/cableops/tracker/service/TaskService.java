package com.cableops.tracker.service;

import com.cableops.tracker.dto.TaskCommentDto;
import com.cableops.tracker.dto.TaskCommentRequest;
import com.cableops.tracker.dto.TaskRequest;
import com.cableops.tracker.dto.TaskResponse;
import com.cableops.tracker.entity.Attachment;
import com.cableops.tracker.entity.EbbMll;
import com.cableops.tracker.entity.Task;
import com.cableops.tracker.entity.TaskCircuit;
import com.cableops.tracker.entity.TaskComment;
import com.cableops.tracker.entity.TaskEbbMll;
import com.cableops.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository        taskRepo;
	private final TaskCommentRepository commentRepo;
	private final TaskCircuitRepository taskCircuitRepo;
	private final TaskEbbMllRepository  taskEbbMllRepo;
	private final OhfCircuitRepository  circuitRepo;
	private final EbbMllRepository      ebbMllRepo;
	private final UserRepository        userRepo;
	private final AccountRepository     accountRepo;
	private final TelegramService       telegramService;
	private final AttachmentRepository  attachmentRepo;   // ← NEW

	// ── CREATE ────────────────────────────────────────────────────────────────
	@Transactional
	public TaskResponse create(TaskRequest req) {
		Task t = new Task();
		t.setId(UUID.randomUUID().toString());
		t.setCSRNumber(generateSRNumber());
		t.setCreatedAt(LocalDateTime.now());
		t.setModifiedAt(LocalDateTime.now());
		map(t, req);

		// Resolve account from parent — OHF circuit or EBB/MLL
		if (req.getParentId() != null) {
			if ("EBBMLLs".equals(req.getParentType())) {
				ebbMllRepo.findById(req.getParentId()).ifPresent(e -> {
					t.setAccountId(e.getAccountId());
					t.setAccountName(e.getAccountName());
				});
			} else {
				circuitRepo.findById(req.getParentId()).ifPresent(c -> {
					t.setAccountId(c.getAccountId());
					t.setAccountName(c.getAccountName());
				});
			}
		}

		taskRepo.save(t);
		saveCircuits(t.getId(), req.getCOHFCircuitsesIds(), req.getCOHFCircuitsesNames());
		saveEbbMlls(t.getId(), req.getCEBBMLLsIds(), req.getCEBBMLLsNames());

		// ── Re-link any attachments uploaded with relatedId="temp" ────────────
		linkAttachmentsToTask(t.getId(), req.getAttachmentsIds());

		logCreate(t.getId(), req);

		// ── Telegram notifications ────────────────────────────────────────────
		String msg = buildCreateMessage(t, req);
		notifyAllUsers(req.getAssignedUserId(), req.getCSecondaryAssignedUserIds(), msg);
		notifyAccountGroup(t.getAccountId(), msg);

		return toResponseFromDb(t);
	}

	// ── GET ───────────────────────────────────────────────────────────────────
	public TaskResponse get(String id) {
		Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found: " + id));
		return toResponseFromDb(t);
	}

	// ── LIST ──────────────────────────────────────────────────────────────────
	public List<TaskResponse> list(String status, String assignedUserId, String accountId) {
		List<Task> tasks;
		if (status != null)
			tasks = taskRepo.findByStatus(status);
		else if (assignedUserId != null)
			tasks = taskRepo.findByAssignedUserId(assignedUserId);
		else if (accountId != null)
			tasks = taskRepo.findByAccountId(accountId);
		else
			tasks = taskRepo.findAll();

		return tasks.stream().map(t -> {
			try {
				return toResponseFromDb(t);
			} catch (Exception e) {
				log.error("Error mapping task {}: {}", t.getId(), e.getMessage());
				return baseFields(t);
			}
		}).toList();
	}

	// ── UPDATE ────────────────────────────────────────────────────────────────
	@Transactional
	public TaskResponse update(String id, TaskRequest req) {
		Task old = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found: " + id));

		String oldStatus = old.getStatus();
		Map<String, Boolean> changedFields = detectChanges(old, req);

		validateMaterialOnComplete(old, req);
		map(old, req);
		old.setModifiedAt(LocalDateTime.now());

		if (req.getParentId() != null) {
			if ("EBBMLLs".equals(req.getParentType())) {
				ebbMllRepo.findById(req.getParentId()).ifPresent(e -> {
					old.setAccountId(e.getAccountId());
					old.setAccountName(e.getAccountName());
				});
			} else {
				circuitRepo.findById(req.getParentId()).ifPresent(c -> {
					old.setAccountId(c.getAccountId());
					old.setAccountName(c.getAccountName());
				});
			}
		}

		taskRepo.save(old);

		if (req.getCOHFCircuitsesIds() != null) {
			taskCircuitRepo.deleteByTaskId(id);
			saveCircuits(id, req.getCOHFCircuitsesIds(), req.getCOHFCircuitsesNames());
		}
		if (req.getCEBBMLLsIds() != null) {
			taskEbbMllRepo.deleteByTaskId(id);
			saveEbbMlls(id, req.getCEBBMLLsIds(), req.getCEBBMLLsNames());
		}

		// ── Reconcile attachments — delete removed, link new ──────────────────
		if (req.getAttachmentsIds() != null) {
			reconcileAttachments(id, req.getAttachmentsIds());
		}

		// Status-change detection: ONLY when truly different
		String requestedStatus = req.getStatus();
		boolean statusActuallyChanged = requestedStatus != null && !requestedStatus.equals(oldStatus);

		if (statusActuallyChanged) {
			logStatus(id, req, requestedStatus);
		}

		List<String> updatedFieldNames = changedFields.entrySet().stream().filter(Map.Entry::getValue)
				.map(Map.Entry::getKey).collect(Collectors.toList());
		if (!updatedFieldNames.isEmpty()) {
			logUpdate(id, req, updatedFieldNames);
		}

		// ── Telegram notifications ────────────────────────────────────────────
		// Skip the whole notification only if literally nothing changed.
		if (statusActuallyChanged || !updatedFieldNames.isEmpty()) {
			List<String> secondaryIds = resolveSecondaryIds(old, req);

			// Pass the actual status delta (null if unchanged) so the message
			// header is "Task Status Updated" only on real status changes.
			String statusForMsg = statusActuallyChanged ? requestedStatus : null;

			String msg = buildUpdateMessage(old, req, statusForMsg, updatedFieldNames);
			notifyAllUsers(old.getAssignedUserId(), secondaryIds, msg);
			notifyAccountGroup(old.getAccountId(), msg);
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
		commentRepo.findByTaskIdOrderByCreatedAtAsc(id).forEach(c -> commentRepo.deleteById(c.getId()));

		// Also clean up attachment rows + files for this task
		List<Attachment> linked = attachmentRepo.findByRelatedIdAndRelatedType(id, "Task");
		for (Attachment a : linked) {
			deleteAttachmentFile(a);
			attachmentRepo.deleteById(a.getId());
		}

		taskRepo.deleteById(id);
	}

	// ── ADD COMMENT ───────────────────────────────────────────────────────────
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
		commentRepo.save(c);
		return toCommentDto(c);
	}

	// ── ATTACHMENT HELPERS ────────────────────────────────────────────────────

	/**
	 * Re-point any attachments uploaded with relatedId="temp" to the new task ID.
	 * Used during task creation, where the upload happens before the task exists.
	 */
	private void linkAttachmentsToTask(String taskId, List<String> attachmentIds) {
		if (attachmentIds == null || attachmentIds.isEmpty()) return;
		for (String attId : attachmentIds) {
			if (attId == null || attId.isBlank()) continue;
			attachmentRepo.findById(attId).ifPresent(a -> {
				if ("temp".equals(a.getRelatedId()) || a.getRelatedId() == null) {
					a.setRelatedId(taskId);
					a.setRelatedType("Task");
					attachmentRepo.save(a);
				}
			});
		}
	}

	/**
	 * On update: compare currently-linked attachments to the request list.
	 * Delete any that have been removed (DB row + file on disk).
	 * The request never INSERTs new rows — those were created by the upload
	 * endpoint at the moment the user clicked "Attach File".
	 */
	private void reconcileAttachments(String taskId, List<String> requestedIds) {
		List<Attachment> currentlyLinked =
				attachmentRepo.findByRelatedIdAndRelatedType(taskId, "Task");

		Set<String> keepIds = new HashSet<>(requestedIds);

		for (Attachment a : currentlyLinked) {
			if (!keepIds.contains(a.getId())) {
				deleteAttachmentFile(a);
				attachmentRepo.deleteById(a.getId());
				log.info("Removed attachment {} from task {}", a.getId(), taskId);
			}
		}

		// Also link any "temp" rows the user just uploaded into this task ID
		// (e.g. if they uploaded before clicking save on a freshly opened edit page).
		linkAttachmentsToTask(taskId, requestedIds);
	}

	/**
	 * Best-effort file deletion. Logs but never throws.
	 */
	private void deleteAttachmentFile(Attachment a) {
		if (a.getPath() == null || a.getPath().isBlank()) return;
		try {
			Path p = Paths.get(a.getPath());
			Files.deleteIfExists(p);
		} catch (Exception ex) {
			log.warn("Could not delete attachment file {}: {}", a.getPath(), ex.getMessage());
		}
	}

	// ── TELEGRAM HELPERS ──────────────────────────────────────────────────────

	/**
	 * Notify the primary assigned user + all secondary users.
	 */
	private void notifyAllUsers(String assignedUserId, List<String> secondaryIds, String message) {
		notifyUser(assignedUserId, message);
		if (secondaryIds != null) {
			secondaryIds.stream().filter(id -> id != null && !id.isBlank()).forEach(id -> notifyUser(id, message));
		}
	}

	/**
	 * Lookup the user's telegramUsername and send a private message.
	 */
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

	/**
	 * Lookup the account's Telegram group chat ID and send there.
	 */
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

		// Header switches based on whether status actually changed
		String header = (newStatus != null && !newStatus.isBlank()) ? "📌 Task Status Updated" : "📌 Task Updated";

		StringBuilder footer = new StringBuilder();
		if (newStatus != null && !newStatus.isBlank()) {
			footer.append("\n<b>🔔 Status changed to:</b> ").append(newStatus);
		}
		if (changedFields != null && !changedFields.isEmpty()) {
			footer.append("\n<b>Updated Fields:</b> ").append(String.join(", ", changedFields));
		}
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
		if (circuitId != null && !circuitId.isBlank()) {
			sb.append("<b>Circuit ID:</b> ").append(circuitId).append("\n");
		}

		String assignedName = coalesce(req != null ? req.getAssignedUserName() : null, t.getAssignedUserName());
		if (assignedName != null && !assignedName.isBlank()) {
			sb.append("<b>Assigned User:</b> ").append(assignedName).append("\n");
		}

		String secondaryNames = resolveSecondaryNamesString(t, req);
		if (secondaryNames != null && !secondaryNames.isBlank()) {
			sb.append("<b>Secondary Assigned User:</b> ").append(secondaryNames).append("\n");
		}

		String note = req != null && req.getCNote() != null ? req.getCNote() : t.getCNote();
		if (!isBlank(note)) {
			sb.append("<b>Note:</b> ").append(note).append("\n");
		}

		String description = req != null && req.getDescription() != null ? req.getDescription() : t.getDescription();
		if (!isBlank(description)) {
			sb.append("<b>Description:</b> ").append(description);
			if (!description.endsWith("\n"))
				sb.append("\n");
		}

		// ─── Material Used block ──────────────────────────────────────────────
		String materialBlock = buildMaterialUsedBlock(t, req);
		if (materialBlock != null && !materialBlock.isBlank()) {
			sb.append("\n").append(materialBlock);
		}

		if (footer != null && !footer.isBlank()) {
			sb.append(footer);
		}
		return sb.toString();
	}

	/**
	 * Build the "Material Used" block for the Telegram message. Returns null if no
	 * material values are present.
	 */
	private String buildMaterialUsedBlock(Task t, TaskRequest req) {

		String  ofcType    = pickStr(req != null ? req.getOfcType()         : null, t.getOfcType());
		Double  ofcStart   = pickDbl(req != null ? req.getOfcStartingMtr()  : null, t.getOfcStartingMtr());
		Double  ofcEnd     = pickDbl(req != null ? req.getOfcEndingMtr()    : null, t.getOfcEndingMtr());
		Double  fiberUsed  = pickDbl(req != null ? req.getFiberUsedMtr()    : null, t.getFiberUsedMtr());
		Integer mediumBox  = pickInt(req != null ? req.getMediumJcBoxUsed() : null, t.getMediumJcBoxUsed());
		Integer smallBox   = pickInt(req != null ? req.getSmallJcBoxUsed()  : null, t.getSmallJcBoxUsed());
		Integer patchCable = pickInt(req != null ? req.getPatchCableUsed()  : null, t.getPatchCableUsed());

		boolean any = (ofcType != null && !ofcType.isBlank()) || ofcStart != null || ofcEnd != null || fiberUsed != null
				|| mediumBox != null || smallBox != null || patchCable != null;
		if (!any) return null;

		// Auto-derive Fiber Used if start/end given but value missing
		if (fiberUsed == null && ofcStart != null && ofcEnd != null) {
			fiberUsed = Math.abs(ofcEnd - ofcStart);
		}

		StringBuilder b = new StringBuilder();
		b.append("<b>📦 Material Used</b>\n");
		if (ofcType != null && !ofcType.isBlank())
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

	private static String pickStr(String fromReq, String fromDb) {
		return (fromReq != null && !fromReq.isBlank()) ? fromReq : fromDb;
	}

	private static Double pickDbl(Double fromReq, Double fromDb) {
		return fromReq != null ? fromReq : fromDb;
	}

	private static Integer pickInt(Integer fromReq, Integer fromDb) {
		return fromReq != null ? fromReq : fromDb;
	}

	private static String fmtNum(Double d) {
		if (d == null) return "—";
		if (d == Math.floor(d) && !Double.isInfinite(d)) {
			return String.valueOf(d.longValue());
		}
		return String.valueOf(d);
	}

	private String extractCircuitId(String description) {
		if (description == null || description.isBlank()) return null;
		for (String line : description.split("\\R")) {
			String trimmed = line.trim();
			if (trimmed.regionMatches(true, 0, "Circuit ID:", 0, "Circuit ID:".length())) {
				String value = trimmed.substring("Circuit ID:".length()).trim();
				return value.isEmpty() ? null : value;
			}
		}
		return null;
	}

	private String resolveSecondaryNamesString(Task t, TaskRequest req) {
		if (req != null && req.getCSecondaryAssignedUserIds() != null && !req.getCSecondaryAssignedUserIds().isEmpty()
				&& req.getCSecondaryAssignedUserNames() != null && !req.getCSecondaryAssignedUserNames().isEmpty()) {
			String joined = req.getCSecondaryAssignedUserIds().stream()
					.map(id -> req.getCSecondaryAssignedUserNames().getOrDefault(id, ""))
					.filter(n -> n != null && !n.isBlank()).collect(Collectors.joining(", "));
			if (!joined.isBlank()) return joined;
		}
		if (t.getCSecondaryUserNames() != null && !t.getCSecondaryUserNames().isBlank()) {
			return Arrays.stream(t.getCSecondaryUserNames().split(","))
					.map(String::trim).filter(n -> !n.isBlank())
					.collect(Collectors.joining(", "));
		}
		if (t.getCSecondaryAssignedUserName() != null && !t.getCSecondaryAssignedUserName().isBlank()) {
			return t.getCSecondaryAssignedUserName();
		}
		return null;
	}

	// ── ACTIVITY LOG HELPERS ──────────────────────────────────────────────────

	private void logCreate(String taskId, TaskRequest req) {
		String assignedName = req.getAssignedUserName() != null ? req.getAssignedUserName() : "Unassigned";
		commentRepo.save(buildLog(taskId, req, "create", "{\"assignedTo\":\"" + esc(assignedName) + "\","
				+ "\"status\":\"" + esc(coalesce(req.getStatus(), "New")) + "\"}"));
	}

	private void logStatus(String taskId, TaskRequest req, String newStatus) {
		commentRepo.save(buildLog(taskId, req, "status", "{\"status\":\"" + esc(newStatus) + "\"}"));
	}

	private void logUpdate(String taskId, TaskRequest req, List<String> fields) {
		String fieldsJson = fields.stream().map(f -> "\"" + esc(f) + "\"").collect(Collectors.joining(",", "[", "]"));
		commentRepo.save(buildLog(taskId, req, "update", "{\"fields\":" + fieldsJson + "}"));
	}

	private TaskComment buildLog(String taskId, TaskRequest req, String type, String data) {
		TaskComment c = new TaskComment();
		c.setId(UUID.randomUUID().toString());
		c.setTaskId(taskId);
		c.setUserId(req.getAssignedUserId());
		c.setUserName(req.getAssignedUserName());
		c.setType(type);
		c.setData(data);
		c.setCreatedAt(LocalDateTime.now());
		return c;
	}

	// ── CHANGE DETECTION ─────────────────────────────────────────────────────

	private Map<String, Boolean> detectChanges(Task old, TaskRequest req) {
		Map<String, Boolean> m = new LinkedHashMap<>();
		m.put("RFO",                     changed(old.getCRFO(),             req.getCRFO()));
		m.put("Fiber Used (Mtr)",        changed(old.getFiberUsedMtr(),     req.getFiberUsedMtr()));
		m.put("OFC Starting Mtr",        changed(old.getOfcStartingMtr(),   req.getOfcStartingMtr()));
		m.put("OFC Ending Mtr",          changed(old.getOfcEndingMtr(),     req.getOfcEndingMtr()));
		m.put("Medium JC Box Used (No)", changed(old.getMediumJcBoxUsed(),  req.getMediumJcBoxUsed()));
		m.put("Small JC Box Used (No)",  changed(old.getSmallJcBoxUsed(),   req.getSmallJcBoxUsed()));
		m.put("Patch Cable Used (No)",   changed(old.getPatchCableUsed(),   req.getPatchCableUsed()));
		m.put("Description",             changed(old.getDescription(),      req.getDescription()));
		m.put("Note",                    changed(old.getCNote(),            req.getCNote()));
		m.put("Assigned User",           changed(old.getAssignedUserId(),   req.getAssignedUserId()));
		m.put("Date Start",              changed(old.getDateStart(),        req.getDateStart()));
		m.put("Date Completed",          changed(old.getDateCompleted(),    req.getDateCompleted()));
		return m;
	}

	private boolean changed(Object oldVal, Object newVal) {
		if (newVal == null) return false;
		return !newVal.equals(oldVal);
	}

	// ── MAPPING HELPERS ───────────────────────────────────────────────────────

	private String generateSRNumber() {
		long count = taskRepo.count() + 1;
		return "SR" + String.format("%04d", count);
	}

	private void validateMaterialOnComplete(Task current, TaskRequest req) {
		String newStatus = req.getStatus();
		if (!"Completed".equals(newStatus)) return;
		String workType = req.getCWorkType() != null ? req.getCWorkType() : current.getCWorkType();
		if ("Survey".equalsIgnoreCase(workType)) return;
		String  ofcType    = req.getOfcType()         != null ? req.getOfcType()         : current.getOfcType();
		Double  ofcStart   = req.getOfcStartingMtr()  != null ? req.getOfcStartingMtr()  : current.getOfcStartingMtr();
		Double  ofcEnd     = req.getOfcEndingMtr()    != null ? req.getOfcEndingMtr()    : current.getOfcEndingMtr();
		Integer mediumBox  = req.getMediumJcBoxUsed() != null ? req.getMediumJcBoxUsed() : current.getMediumJcBoxUsed();
		Integer smallBox   = req.getSmallJcBoxUsed()  != null ? req.getSmallJcBoxUsed()  : current.getSmallJcBoxUsed();
		Integer patchCable = req.getPatchCableUsed() != null ? req.getPatchCableUsed()  : current.getPatchCableUsed();
		StringBuilder missing = new StringBuilder();
		if (ofcType    == null || ofcType.isBlank()) missing.append("OFC Type, ");
		if (ofcStart   == null) missing.append("OFC Starting Mtr, ");
		if (ofcEnd     == null) missing.append("OFC Ending Mtr, ");
		if (mediumBox  == null) missing.append("Medium JC Box Used, ");
		if (smallBox   == null) missing.append("Small JC Box Used, ");
		if (patchCable == null) missing.append("Patch Cable Used, ");
		if (missing.length() > 0) {
			String fields = missing.substring(0, missing.length() - 2);
			throw new RuntimeException("Cannot complete task: Material Used fields are required — " + fields);
		}
	}

	private void map(Task t, TaskRequest r) {
		if (r.getName()          != null) t.setName(r.getName());
		if (r.getStatus()        != null) t.setStatus(r.getStatus());
		else if (t.getStatus()   == null) t.setStatus("New");
		if (r.getPriority()      != null) t.setPriority(r.getPriority());
		else if (t.getPriority() == null) t.setPriority("Normal");
		if (r.getCWorkType()     != null) t.setCWorkType(r.getCWorkType());
		if (r.getCRFO()          != null) t.setCRFO(r.getCRFO());
		if (r.getParentId()      != null) t.setParentId(r.getParentId());
		if (r.getParentType()    != null) t.setParentType(r.getParentType());
		if (r.getParentName()    != null) t.setParentName(r.getParentName());
		if (r.getDateStart()     != null) t.setDateStart(r.getDateStart());
		if (r.getDateStartDate() != null) t.setDateStartDate(r.getDateStartDate());
		if (r.getDateCompleted() != null) t.setDateCompleted(r.getDateCompleted());
		if (r.getCDurationText() != null) t.setCDurationText(r.getCDurationText());
		if (r.getDescription()   != null) t.setDescription(r.getDescription());
		if (r.getCNote()         != null) t.setCNote(r.getCNote());
		if (r.getAssignedUserId()   != null) t.setAssignedUserId(r.getAssignedUserId());
		if (r.getAssignedUserName() != null) t.setAssignedUserName(r.getAssignedUserName());
		if (r.getOfcType()         != null) t.setOfcType(r.getOfcType());
		if (r.getOfcStartingMtr()  != null) t.setOfcStartingMtr(r.getOfcStartingMtr());
		if (r.getOfcEndingMtr()    != null) t.setOfcEndingMtr(r.getOfcEndingMtr());
		if (r.getFiberUsedMtr()    != null) t.setFiberUsedMtr(r.getFiberUsedMtr());
		if (r.getMediumJcBoxUsed() != null) t.setMediumJcBoxUsed(r.getMediumJcBoxUsed());
		if (r.getSmallJcBoxUsed()  != null) t.setSmallJcBoxUsed(r.getSmallJcBoxUsed());
		if (r.getPatchCableUsed()  != null) t.setPatchCableUsed(r.getPatchCableUsed());
		if (r.getCFieldNotes()     != null) t.setCFieldNotes(r.getCFieldNotes());
		if (r.getCResolutionNotes() != null) t.setCResolutionNotes(r.getCResolutionNotes());
		if (r.getAcceptanceTimeMins() != null) t.setAcceptanceTimeMins(r.getAcceptanceTimeMins());
		// ── Multiple secondary users ───────────────────────────────────────────
		if (r.getCSecondaryAssignedUserIds() != null) {
			t.setCSecondaryUserIds(String.join(",", r.getCSecondaryAssignedUserIds()));
		}
		if (r.getCSecondaryAssignedUserNames() != null && !r.getCSecondaryAssignedUserNames().isEmpty()) {
			// Preserve order matching IDs list
			if (r.getCSecondaryAssignedUserIds() != null) {
				String names = r.getCSecondaryAssignedUserIds().stream()
						.map(id -> r.getCSecondaryAssignedUserNames().getOrDefault(id, ""))
						.collect(Collectors.joining(","));
				t.setCSecondaryUserNames(names);
			}
		}
	}

	/**
	 * Resolve secondary user IDs for notifications. Use request value if provided;
	 * fall back to what's stored in DB.
	 */
	private List<String> resolveSecondaryIds(Task saved, TaskRequest req) {
		if (req.getCSecondaryAssignedUserIds() != null) {
			return req.getCSecondaryAssignedUserIds();
		}
		if (saved.getCSecondaryUserIds() != null && !saved.getCSecondaryUserIds().isBlank()) {
			return Arrays.asList(saved.getCSecondaryUserIds().split(","));
		}
		// Legacy single secondary
		if (saved.getCSecondaryAssignedUserId() != null) {
			return List.of(saved.getCSecondaryAssignedUserId());
		}
		return List.of();
	}

	private void saveCircuits(String taskId, List<String> ids, Map<String, String> names) {
		if (ids == null || ids.isEmpty()) return;
		Map<String, String> seen = new LinkedHashMap<>();
		for (String cid : ids) {
			seen.putIfAbsent(cid, names != null ? names.getOrDefault(cid, "") : "");
		}
		seen.forEach((cid, cname) -> taskCircuitRepo.save(new TaskCircuit(null, taskId, cid, cname)));
	}

	private void saveEbbMlls(String taskId, List<String> ids, Map<String, String> names) {
		if (ids == null || ids.isEmpty()) return;
		Map<String, String> seen = new LinkedHashMap<>();
		for (String eid : ids) {
			seen.putIfAbsent(eid, names != null ? names.getOrDefault(eid, "") : "");
		}
		seen.forEach((eid, ename) -> taskEbbMllRepo.save(new TaskEbbMll(null, taskId, eid, ename)));
	}

	// ── RESPONSE BUILDERS ─────────────────────────────────────────────────────

	private TaskResponse toResponseFromDb(Task t) {
		TaskResponse res = baseFields(t);

		// ── OHF Circuits (with live name fallback) ────────────────────────────
		List<TaskCircuit> circuits = taskCircuitRepo.findByTaskId(t.getId());
		res.setCOHFCircuitsesIds(circuits.stream()
				.map(TaskCircuit::getCircuitId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList()));

		Map<String, String> circuitNamesMap = new LinkedHashMap<>();
		for (TaskCircuit c : circuits) {
			if (c.getCircuitId() == null) continue;
			String name = c.getCircuitName();
			if (name == null || name.isBlank()) {
				name = circuitRepo.findById(c.getCircuitId())
						.map(ohf -> ohf.getName())
						.orElse(c.getCircuitId());
			}
			circuitNamesMap.putIfAbsent(c.getCircuitId(), name);
		}
		res.setCOHFCircuitsesNames(circuitNamesMap);

		// ── EBB / MLL (with live name fallback) ───────────────────────────────
		List<TaskEbbMll> ebbMlls = taskEbbMllRepo.findByTaskId(t.getId());
		res.setCEBBMLLsIds(ebbMlls.stream()
				.map(TaskEbbMll::getEbbMllId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList()));

		Map<String, String> ebbMllNamesMap = new LinkedHashMap<>();
		for (TaskEbbMll e : ebbMlls) {
			if (e.getEbbMllId() == null) continue;
			String name = e.getEbbMllName();
			if (name == null || name.isBlank()) {
				name = ebbMllRepo.findById(e.getEbbMllId())
						.map(EbbMll::getName)
						.orElse(e.getEbbMllId());
			}
			ebbMllNamesMap.putIfAbsent(e.getEbbMllId(), name);
		}
		res.setCEBBMLLsNames(ebbMllNamesMap);

		// ── Attachments — derived from `attachments` table ────────────────────
		List<Attachment> taskAttachments =
				attachmentRepo.findByRelatedIdAndRelatedType(t.getId(), "Task");

		res.setAttachmentsIds(taskAttachments.stream()
				.map(Attachment::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toList()));

		Map<String, String> attachNames = new LinkedHashMap<>();
		Map<String, String> attachTypes = new LinkedHashMap<>();
		for (Attachment a : taskAttachments) {
			if (a.getId() == null) continue;
			if (a.getName() != null) attachNames.put(a.getId(), a.getName());
			if (a.getType() != null) attachTypes.put(a.getId(), a.getType());
		}
		res.setAttachmentsNames(attachNames);
		res.setAttachmentsTypes(attachTypes);

		// ── Stream / Comments ─────────────────────────────────────────────────
		List<TaskComment> comments = commentRepo.findByTaskIdOrderByCreatedAtAsc(t.getId());
		res.setStream(comments.stream().map(this::toCommentDto).collect(Collectors.toList()));

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
		res.setCreatedAt(t.getCreatedAt());
		res.setModifiedAt(t.getModifiedAt());
		res.setCreatedById(t.getCreatedById());
		res.setCreatedByName(t.getCreatedByName());
		res.setCFieldNotes(t.getCFieldNotes());
		res.setCResolutionNotes(t.getCResolutionNotes());
		// ── Multiple secondary users ───────────────────────────────────────────
		if (t.getCSecondaryUserIds() != null && !t.getCSecondaryUserIds().isBlank()) {
			List<String> ids = Arrays.asList(t.getCSecondaryUserIds().split(","));
			res.setCSecondaryAssignedUserIds(ids);
			if (t.getCSecondaryUserNames() != null) {
				String[] names = t.getCSecondaryUserNames().split(",", -1);
				Map<String, String> namesMap = new LinkedHashMap<>();
				for (int i = 0; i < ids.size(); i++) {
					namesMap.put(ids.get(i), i < names.length ? names[i] : ids.get(i));
				}
				res.setCSecondaryAssignedUserNames(namesMap);
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
		res.setAcceptanceTimeMins(t.getAcceptanceTimeMins());
		res.setAcceptedAt(t.getAcceptedAt());
		res.setAcceptedById(t.getAcceptedById());
		res.setAcceptedByName(t.getAcceptedByName());
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

	private static String coalesce(String a, String b) {
		return a != null ? a : b;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("\"", "\\\"");
	}
}