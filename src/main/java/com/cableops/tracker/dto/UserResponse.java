package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class UserResponse {

    private String id;
    private String userName;

    private String salutationName;
    private String firstName;
    private String lastName;
    private String name;

    private String type;
    private Boolean isActive;

    private String title;
    private String gender;

    private String cTelegramUsername;
    private String cFathername;
    private String cAadharNumber;
    private String cEmergencyContactNumber;
    private LocalDate cDateofjoining;

    // Teams
    private List<String> teamsIds;
    private Map<String, String> teamsNames;

    // Roles
    private List<String> rolesIds;
    private Map<String, String> rolesNames;

    // Proof documents
    private List<String> cProofDocumentsIds;
    private Map<String, String> cProofDocumentsNames;
    private Map<String, String> cProofDocumentsTypes;

    // Avatar
    private String avatarId;
    private String avatarName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;
}