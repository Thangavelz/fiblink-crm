package com.cableops.tracker.controller;

import com.cableops.tracker.dto.TaskCommentDto;
import com.cableops.tracker.dto.TaskCommentRequest;
import com.cableops.tracker.dto.TaskRequest;
import com.cableops.tracker.dto.TaskResponse;
import com.cableops.tracker.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    // ── CREATE — Admin / Manager only ────────────────────────────────────────
    @PostMapping
    public TaskResponse create(@RequestBody TaskRequest req, Authentication auth) {
        requireRole(auth, "ROLE_ADMIN", "ROLE_MANAGER");
        return service.create(req);
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
            HttpServletResponse response) {

        List<TaskResponse> all = service.list(status, assignedUserId, accountId);

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

        if (hasRole(auth, "ROLE_FIELD_ENGINEER")) {
            // Field Engineer: may only update status and field-level notes.
            // Pass a stripped copy so they cannot reassign or change other metadata.
            TaskRequest stripped = new TaskRequest();
            stripped.setStatus(req.getStatus());
            stripped.setCFieldNotes(req.getCFieldNotes());
            stripped.setCResolutionNotes(req.getCResolutionNotes());
            return service.update(id, stripped);
        }

        if (hasRole(auth, "ROLE_MANAGER")) {
            // Manager: full update including reassign (assignedUserId / assignedUserName)
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
