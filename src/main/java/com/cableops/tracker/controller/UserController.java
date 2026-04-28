package com.cableops.tracker.controller;

import com.cableops.tracker.dto.UserRequest;
import com.cableops.tracker.dto.UserResponse;
import com.cableops.tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    /** POST /api/users  — create a new user */
    @PostMapping
    public UserResponse create(@RequestBody UserRequest req) {
        return service.create(req);
    }

    /** GET /api/users/{id}  — fetch single user */
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable("id") String id) {
        return service.get(id);
    }

    /** GET /api/users  — list all users */
    @GetMapping
    public List<UserResponse> list() {
        return service.list();
    }

    /** PUT /api/users/{id}  — update user (partial: only non-null fields applied) */
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable("id") String id,
                               @RequestBody UserRequest req) {
        return service.update(id, req);
    }

    /** DELETE /api/users/{id}  — delete user */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        service.delete(id);
    }
}