package com.cableops.tracker.dto;

import lombok.Data;
import java.util.List;

@Data
public class TeamRequest {
    private String name;
    private String areaCode;
    private Boolean isActive;

    // Users to add under this team: list of user IDs
    private List<String> userIds;
}