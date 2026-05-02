package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse {
    private String id;
    private String name;
    private String status;
    private String priority;

    @JsonProperty("cWorkType")
    private String cWorkType;
    @JsonProperty("cRFO")
    private String cRFO;
    @JsonProperty("cSRNumber")
    private String cSRNumber;

    private String parentId;
    private String parentType;
    private String parentName;

    @JsonProperty("cOHFCircuitsesIds")
    private List<String> cOHFCircuitsesIds;
    @JsonProperty("cOHFCircuitsesNames")
    private Map<String, String> cOHFCircuitsesNames;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateStart;
    private LocalDate dateStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCompleted;

    @JsonProperty("cDurationText")
    private String cDurationText;
    private String description;

    @JsonProperty("cNote")
    private String cNote;

    private String assignedUserId;
    private String assignedUserName;

    @JsonProperty("cSecondaryAssignedUserIds")
    private List<String> cSecondaryAssignedUserIds;
    @JsonProperty("cSecondaryAssignedUserNames")
    private Map<String, String> cSecondaryAssignedUserNames;

    private String accountId;
    private String accountName;

    private List<String> teamsIds;
    private Map<String, String> teamsNames;

    private List<String> attachmentsIds;
    private Map<String, String> attachmentsNames;
    private Map<String, String> attachmentsTypes;

    private String ofcType;
    private Double ofcStartingMtr;
    private Double ofcEndingMtr;
    private Double fiberUsedMtr;
    private Integer mediumJcBoxUsed;
    private Integer smallJcBoxUsed;
    private Integer patchCableUsed;

    private List<TaskCommentDto> stream;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;

    private String createdById;
    private String createdByName;
    private Integer acceptanceTimeMins;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptedAt;

    private String acceptedById;
    private String acceptedByName;

    @JsonProperty("cEBBMLLsIds")
    private List<String> cEBBMLLsIds;
    @JsonProperty("cEBBMLLsNames")
    private Map<String, String> cEBBMLLsNames;

    @JsonProperty("cFieldNotes")
    private String cFieldNotes;
    @JsonProperty("cResolutionNotes")
    private String cResolutionNotes;
}