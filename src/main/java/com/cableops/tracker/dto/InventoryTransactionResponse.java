package com.cableops.tracker.dto;
 
import lombok.Data;
import java.time.LocalDateTime;
 
@Data
public class InventoryTransactionResponse {
    private String id;
    private String txnType;
    private String itemId;
    private String itemName;
    private String storeAreaCode;
    private String storeName;
    private Double quantityChange;
    private Double quantityAfter;
    private String taskId;
    private String taskName;
    private String performedById;
    private String performedByName;
    private String notes;
    private LocalDateTime createdAt;
}