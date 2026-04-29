package com.cableops.tracker.dto;
 
import lombok.Data;
import java.time.LocalDateTime;
 
@Data
public class InventoryStockResponse {
    private String id;
    private String itemId;
    private String itemName;
    private String itemType;
    private String cableType;
    private Integer coreCount;
    private String boxSize;
    private String unit;
    private String storeAreaCode;
    private String storeName;
    private Double quantityOnHand;
    private Double reorderLevel;
    private Boolean lowStock;
    private LocalDateTime updatedAt;
}
 