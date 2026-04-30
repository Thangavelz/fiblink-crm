package com.cableops.tracker.dto;

import lombok.Data;

@Data
public class AdjustRequest {
    private String itemId;
    private String itemName;       // optional, for audit display
    private String storeAreaCode;
    private Double newQuantity;    // the corrected absolute quantity
    private String notes;          // mandatory validation note (enforced in service)
}