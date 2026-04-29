package com.cableops.tracker.dto;
import lombok.Data;
 
@Data
public class InventoryItemRequest {
    private String itemType;      // OFC_CABLE | JCV_BOX
    private String itemName;
    private String cableType;     // SM | MM | ARMORED (OFC only)
    private Integer coreCount;    // OFC only
    private String boxSize;       // SMALL | MEDIUM | LARGE (JCV only)
    private String unit;          // MTR | PCS
    private String description;
    private Boolean isActive;
}