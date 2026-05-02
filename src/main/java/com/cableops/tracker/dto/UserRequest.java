package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    private String avatarId;
    private String avatarName;

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

    private Boolean sendAccessInfo;
    public String getCTelegramUsername() { return cTelegramUsername; }
    public String getCFathername() { return cFathername; }
    public String getCAadharNumber() { return cAadharNumber; }
    public String getCEmergencyContactNumber() { return cEmergencyContactNumber; }
    public LocalDate getCDateofjoining() { return cDateofjoining; }
    public List<String> getCProofDocumentsIds() { return cProofDocumentsIds; }
    public Map<String, String> getCProofDocumentsNames() { return cProofDocumentsNames; }
    public Map<String, String> getCProofDocumentsTypes() { return cProofDocumentsTypes; }
    
}