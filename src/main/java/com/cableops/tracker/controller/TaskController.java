package com.cableops.tracker.controller;

import com.cableops.tracker.dto.TaskCommentDto;
import com.cableops.tracker.dto.TaskCommentRequest;
import com.cableops.tracker.dto.TaskRequest;
import com.cableops.tracker.dto.TaskResponse;
import com.cableops.tracker.entity.Task;
import com.cableops.tracker.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    // ── Status progression order for Field Engineers ──────────────────────────
    private static final List<String> STATUS_ORDER =
        List.of("New", "Asigned", "Accepted", "In Progress", "Completed");
    // Note: "Pending" is a sideways state — allowed from any active status but
    // not part of the linear order.

    // ── CREATE — Admin / Manager only ────────────────────────────────────────
    @PostMapping
    public TaskResponse create(@RequestBody TaskRequest req, Authentication auth) {
        requireRole(auth, "ROLE_ADMIN", "ROLE_MANAGER");
        return service.create(req);
    }

    // ── ACCEPT — ALL roles ───────────────────────────────────────────────────
    @PostMapping("/{id}/accept")
    public TaskResponse accept(@PathVariable("id") String id,
                               @RequestBody(required = false) Map<String, String> body) {
        return service.acceptTask(id);
    }

    // ── GET (single) — all authenticated roles ────────────────────────────────
    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable("id") String id) {
        return service.get(id);
    }

    // ── LIST — all authenticated roles ───────────────────────────────────────
    @GetMapping
    public List<TaskResponse> list(
            @RequestParam(name = "status",         required = false) String status,
            @RequestParam(name = "assignedUserId", required = false) String assignedUserId,
            @RequestParam(name = "accountId",      required = false) String accountId,
            @RequestParam(name = "_start",         required = false) Integer start,
            @RequestParam(name = "_end",           required = false) Integer end,
            Authentication auth,
            HttpServletResponse response) {

        List<TaskResponse> all;
        if (hasRole(auth, "ROLE_FIELD_ENGINEER")) {
            String userId = auth.getName();
            all = service.listForUser(userId);
        } else {
            all = service.list(status, assignedUserId, accountId);
        }

        response.setHeader("X-Total-Count", String.valueOf(all.size()));
        response.setHeader("Access-Control-Expose-Headers", "X-Total-Count");

        if (start != null && end != null) {
            int s = Math.min(start, all.size());
            int e = Math.min(end,   all.size());
            return all.subList(s, e);
        }
        return all;
    }

    // ── UPDATE — role-aware ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable("id") String id,
                               @RequestBody TaskRequest req,
                               Authentication auth) {

        Task current = service.getTaskEntity(id);

        // ── COMPLETED LOCK — only Admin may edit a completed task ─────────────
        if ("Completed".equalsIgnoreCase(current.getStatus()) && !hasRole(auth, "ROLE_ADMIN")) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "This task is Completed and cannot be modified. Contact an Admin."
            );
        }

        if (hasRole(auth, "ROLE_FIELD_ENGINEER")) {

            // ── Forward-only status enforcement ──────────────────────────────
            String currentStatus = current.getStatus();
            String newStatus     = req.getStatus();

            if (newStatus != null && !newStatus.equals(currentStatus)) {
                boolean movingToPending   = "Pending".equals(newStatus);
                boolean movingFromPending = "Pending".equals(currentStatus);

                int currentIdx = STATUS_ORDER.indexOf(currentStatus);
                int newIdx     = STATUS_ORDER.indexOf(newStatus);

                // From Pending: only allow resuming forward (next in order after where it paused)
                // We don't know the pre-Pending status, so allow any forward move from Pending
                boolean isBackward = !movingToPending
                    && !movingFromPending
                    && currentIdx >= 0
                    && newIdx >= 0
                    && newIdx < currentIdx;

                if (isBackward) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot move a task backward. Status must progress forward: "
                        + "New → Asigned → Accepted → In Progress → Completed.");
                }

                // Pending requires a non-blank reason
                if (movingToPending) {
                    String reason = req.getPendingReason();
                    if (reason == null || reason.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A reason is required when setting the status to Pending.");
                    }
                }
            }

            // ── Strip to allowed fields ───────────────────────────────────────
            TaskRequest stripped = new TaskRequest();
            stripped.setStatus(req.getStatus());
            stripped.setPendingReason(req.getPendingReason());
            stripped.setCNote(req.getCNote());
            stripped.setCFieldNotes(req.getCFieldNotes());
            stripped.setCResolutionNotes(req.getCResolutionNotes());
            stripped.setDescription(req.getDescription());
            stripped.setOfcType(req.getOfcType());
            stripped.setOfcStartingMtr(req.getOfcStartingMtr());
            stripped.setOfcEndingMtr(req.getOfcEndingMtr());
            stripped.setFiberUsedMtr(req.getFiberUsedMtr());
            stripped.setMediumJcBoxUsed(req.getMediumJcBoxUsed());
            stripped.setSmallJcBoxUsed(req.getSmallJcBoxUsed());
            stripped.setPatchCableUsed(req.getPatchCableUsed());
            stripped.setDateCompleted(req.getDateCompleted());
            stripped.setAttachmentsIds(req.getAttachmentsIds());
            stripped.setAttachmentsNames(req.getAttachmentsNames());
            stripped.setAttachmentsTypes(req.getAttachmentsTypes());
         // In TaskController.java update() method, add to the stripped request:
            stripped.setDescription(req.getDescription());   // ← ADD
            stripped.setCNote(req.getCNote());                // already there ✓
            stripped.setCFieldNotes(req.getCFieldNotes());    // already there ✓
            stripped.setCResolutionNotes(req.getCResolutionNotes()); // already there ✓
            return service.update(id, stripped);
        }

        if (hasRole(auth, "ROLE_MANAGER")) {
            return service.update(id, req);
        }

        // ADMIN: unrestricted
        requireRole(auth, "ROLE_ADMIN");
        return service.update(id, req);
    }

    // ── DELETE — Admin only ───────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id, Authentication auth) {
        requireRole(auth, "ROLE_ADMIN");
        service.delete(id);
    }

    // ── COMMENT — all authenticated roles ────────────────────────────────────
    @PostMapping("/{id}/comments")
    public TaskCommentDto addComment(@PathVariable("id") String id,
                                     @RequestBody TaskCommentRequest req) {
        return service.addComment(id, req);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean hasRole(Authentication auth, String role) {
        return auth != null &&
               auth.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }

    private void requireRole(Authentication auth, String... roles) {
        for (String r : roles) {
            if (hasRole(auth, r)) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action");
    }
}