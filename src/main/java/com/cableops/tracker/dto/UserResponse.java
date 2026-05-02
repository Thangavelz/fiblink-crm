package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("cTelegramUsername")
    private String cTelegramUsername;

    @JsonProperty("cFathername")
    private String cFathername;

    @JsonProperty("cAadharNumber")
    private String cAadharNumber;

    @JsonProperty("cEmergencyContactNumber")
    private String cEmergencyContactNumber;

    @JsonProperty("cDateofjoining")
    private LocalDate cDateofjoining;

    private List<String> teamsIds;
    private Map<String, String> teamsNames;
    private List<String> rolesIds;
    private Map<String, String> rolesNames;

    @JsonProperty("cProofDocumentsIds")
    private List<String> cProofDocumentsIds;

    @JsonProperty("cProofDocumentsNames")
    private Map<String, String> cProofDocumentsNames;

    @JsonProperty("cProofDocumentsTypes")
    private Map<String, String> cProofDocumentsTypes;

    private String avatarId;
    private String avatarName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;
}