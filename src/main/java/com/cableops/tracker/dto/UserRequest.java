package com.cableops.tracker.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class UserRequest {

    private String userName;
    private String password;
    private String passwordConfirm;

    private String salutationName;
    private String firstName;
    private String lastName;
    private String name;

    /**
     * Allowed values: "ADMIN", "Manager", "Field Engineer"
     * Validated in UserService before saving.
     */
    private String type;
    private Boolean isActive;

    private String title;
    private String gender;

    private String cTelegramUsername;
    private String cFathername;
    private String cAadharNumber;
    private String cEmergencyContactNumber;
    private LocalDate cDateofjoining;

    // Avatar — upload via /api/v1/Attachment first, then pass the returned id here
    private String avatarId;
    private String avatarName;

    // Teams (pre-loaded list in UI, area-based)
    private List<String> teamsIds;
    private Map<String, String> teamsNames;

    // Roles — mirrors "type"; kept separate so a user can have multiple roles later
    private List<String> rolesIds;
    private Map<String, String> rolesNames;

    // Proof documents — upload via /api/v1/Attachment, pass ids here
    private List<String> cProofDocumentsIds;
    private Map<String, String> cProofDocumentsNames;
    private Map<String, String> cProofDocumentsTypes;

    // UI-only flag; ignored by backend (no email/SMS infra yet)
    private Boolean sendAccessInfo;
}