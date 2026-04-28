package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeamResponse {
    private String id;
    private String name;
    private String areaCode;
    private Boolean isActive;

    // Members of this team
    private List<TeamMember> users;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Data
    public static class TeamMember {
        private String userId;
        private String userName;
        private String name;
        private String avatarId;
    }
}