package com.cableops.tracker.controller;

import com.cableops.tracker.dto.OhfCircuitRequest;
import com.cableops.tracker.dto.OhfCircuitResponse;
import com.cableops.tracker.service.OhfCircuitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ohf-circuits")
@RequiredArgsConstructor
public class OhfCircuitController {

    private final OhfCircuitService service;

    /** POST /api/ohf-circuits */
    @PostMapping
    public OhfCircuitResponse create(@RequestBody OhfCircuitRequest req) {
        return service.create(req);
    }

    /** GET /api/ohf-circuits/{id} — lookup by UUID */
    @GetMapping("/{id}")
    public OhfCircuitResponse get(@PathVariable("id") String id) {
        return service.get(id);
    }

    /** GET /api/ohf-circuits/circuit-id/{circuitId} — lookup by serial e.g. 20260002 */
    @GetMapping("/circuit-id/{circuitId}")
    public OhfCircuitResponse getByCircuitId(@PathVariable("circuitId") String circuitId) {
        return service.getByCircuitId(circuitId);
    }

    /** GET /api/ohf-circuits  or  GET /api/ohf-circuits?accountId=xxx */
    @GetMapping
    public List<OhfCircuitResponse> list(
            @RequestParam(name = "accountId", required = false) String accountId) {
        if (accountId != null) {
            return service.listByAccount(accountId);
        }
        return service.list();
    }

    /** PUT /api/ohf-circuits/{id} */
    @PutMapping("/{id}")
    public OhfCircuitResponse update(@PathVariable("id") String id,
                                     @RequestBody OhfCircuitRequest req) {
        return service.update(id, req);
    }

    /** DELETE /api/ohf-circuits/{id} */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        service.delete(id);
    }
}