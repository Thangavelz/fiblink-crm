package com.cableops.tracker.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class EbbMllRequest {

    private String name;

    private String accountId;
    private String accountName;

    private String addressStreet;
    private String addressCity;
    private String addressState;
    private String addressPostalCode;
    private String addressCountry;

    /** EBB or MLL */
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
}
