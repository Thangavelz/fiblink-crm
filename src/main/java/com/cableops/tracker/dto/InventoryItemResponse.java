package com.cableops.tracker.dto;
 
import lombok.Data;
import java.time.LocalDateTime;
 
@Data
public class InventoryItemResponse {
    private String id;
    private String itemType;
    private String itemName;
    private String cableType;
    private Integer coreCount;
    private String boxSize;
    private String unit;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}