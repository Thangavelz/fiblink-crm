package com.cableops.tracker.dto;

import lombok.Data;

@Data
public class InwardRequest {
    private String itemId;
    private String itemName;       // sent by frontend (e.g. "OFC Cable - 4F")
    private String storeAreaCode;
    private String storeName;      // sent by frontend (team name)
    private Double quantity;
    private String notes;
}