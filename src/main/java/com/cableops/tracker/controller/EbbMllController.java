package com.cableops.tracker.controller;

import com.cableops.tracker.dto.EbbMllRequest;
import com.cableops.tracker.dto.EbbMllResponse;
import com.cableops.tracker.service.EbbMllService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ebb-mll")
@RequiredArgsConstructor
public class EbbMllController {

    private final EbbMllService service;

    @PostMapping
    public EbbMllResponse create(@RequestBody EbbMllRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public EbbMllResponse get(@PathVariable("id") String id) {
        return service.get(id);
    }

    @GetMapping
    public List<EbbMllResponse> list(
            @RequestParam(name = "productType",    required = false) String productType,
            @RequestParam(name = "accountId",      required = false) String accountId,
            @RequestParam(name = "assignedUserId", required = false) String assignedUserId,
            @RequestParam(name = "_start",         required = false) Integer start,
            @RequestParam(name = "_end",           required = false) Integer end,
            HttpServletResponse response) {

        List<EbbMllResponse> all = service.list(productType, accountId, assignedUserId);

        response.setHeader("X-Total-Count", String.valueOf(all.size()));
        response.setHeader("Access-Control-Expose-Headers", "X-Total-Count");

        if (start != null && end != null) {
            int s = Math.min(start, all.size());
            int e = Math.min(end,   all.size());
            return all.subList(s, e);
        }
        return all;
    }

    @PutMapping("/{id}")
    public EbbMllResponse update(@PathVariable("id") String id,
                                  @RequestBody EbbMllRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        service.delete(id);
    }
}