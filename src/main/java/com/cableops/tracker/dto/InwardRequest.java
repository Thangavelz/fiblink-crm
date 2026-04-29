package com.cableops.tracker.dto;
 
import lombok.Data;
 
@Data
public class InwardRequest {
    private String itemId;
    private String storeAreaCode;
    private Double quantity;
    private String notes;
}