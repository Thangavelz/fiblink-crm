package com.cableops.tracker.controller;

import com.cableops.tracker.dto.TeamRequest;
import com.cableops.tracker.dto.TeamResponse;
import com.cableops.tracker.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService service;

    @PostMapping
    public TeamResponse create(@RequestBody TeamRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public TeamResponse get(@PathVariable("id") String id) {
        return service.get(id);
    }

    @GetMapping
    public List<TeamResponse> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public TeamResponse update(@PathVariable("id") String id, @RequestBody TeamRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        service.delete(id);
    }
}