package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class EbbMllResponse {

    private String id;
    private String name;

    private String accountId;
    private String accountName;

    private String addressStreet;
    private String addressCity;
    private String addressState;
    private String addressPostalCode;
    private String addressCountry;

    private String productType;

    private String customerLocation;
    private String customerLatLong;
    private String spocLcDetails;
    private String description;

    // Assigned user — optional
    private String assignedUserId;
    private String assignedUserName;

    // Teams
    private List<String>        teamsIds;
    private Map<String, String> teamsNames;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;

    private String createdById;
    private String createdByName;
}
