package com.cableops.tracker.controller;
 
import com.cableops.tracker.dto.InventoryItemRequest;
import com.cableops.tracker.dto.InventoryItemResponse;
import com.cableops.tracker.service.InventoryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/inventory/items")
@RequiredArgsConstructor
public class InventoryControllers {
 
    private final InventoryService service;
 
    @PostMapping
    public InventoryItemResponse create(@RequestBody InventoryItemRequest req) {
        return service.createItem(req);
    }
 
    @GetMapping("/{id}")
    public InventoryItemResponse get(@PathVariable String id) {
        return service.getItem(id);
    }
 
    @GetMapping
    public List<InventoryItemResponse> list(
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            HttpServletResponse response) {
        List<InventoryItemResponse> items = service.listItems(itemType, activeOnly);
        response.setHeader("X-Total-Count", String.valueOf(items.size()));
        response.setHeader("Access-Control-Expose-Headers", "X-Total-Count");
        return items;
    }
 
    @PutMapping("/{id}")
    public InventoryItemResponse update(@PathVariable String id,
                                        @RequestBody InventoryItemRequest req) {
        return service.updateItem(id, req);
    }
}