package com.cableops.tracker.dto;
 
import lombok.Data;
 
@Data
public class UsageRequest {
    private String itemId;
    private String storeAreaCode;
    private Double quantity;
    private String taskId;
    private String taskName;
    private String notes;
}