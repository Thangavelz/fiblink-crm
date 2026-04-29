package com.cableops.tracker.controller;
 
import com.cableops.tracker.dto.InventoryStockResponse;
import com.cableops.tracker.service.InventoryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.Map;
 
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryStockController {
 
    private final InventoryService service;
 
    @GetMapping("/stock")
    public List<InventoryStockResponse> stock(
            @RequestParam(name = "storeAreaCode", required = false) String storeAreaCode,
            @RequestParam(name = "itemType",      required = false) String itemType,
            HttpServletResponse response) {
 
        List<InventoryStockResponse> list = service.listStock(storeAreaCode, itemType);
        response.setHeader("X-Total-Count", String.valueOf(list.size()));
        response.setHeader("Access-Control-Expose-Headers", "X-Total-Count");
        return list;
    }
 
    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam(name = "storeAreaCode", required = false) String storeAreaCode) {
        return service.summary(storeAreaCode);
    }
}