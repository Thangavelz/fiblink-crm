package com.cableops.tracker.controller;
 
import com.cableops.tracker.dto.*;
import com.cableops.tracker.service.InventoryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryTransactionController {
 
    private final InventoryService service;
 
    /** Record stock received into a store (admin only) */
    @PostMapping("/inward")
    public InventoryTransactionResponse inward(@RequestBody InwardRequest req) {
        return service.recordInward(req);
    }
 
    /** Record material used on a task (any authenticated user) */
    @PostMapping("/usage")
    public InventoryTransactionResponse usage(@RequestBody UsageRequest req) {
        return service.recordUsage(req);
    }
 
    /** Audit log — ADMIN only (enforce in SecurityConfig or check role in service) */
    @GetMapping("/audit")
    public List<InventoryTransactionResponse> audit(
            @RequestParam(required = false) String storeAreaCode,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String txnType,
            HttpServletResponse response) {
        List<InventoryTransactionResponse> list =
                service.listTransactions(storeAreaCode, itemId, txnType);
        response.setHeader("X-Total-Count", String.valueOf(list.size()));
        response.setHeader("Access-Control-Expose-Headers", "X-Total-Count");
        return list;
    }
}