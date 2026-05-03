package com.cableops.tracker.dto;

import com.cableops.tracker.config.FlexibleDateDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskRequest {
    private String name;
    private String status;
    private String priority;

    @JsonProperty("cWorkType")
    private String cWorkType;
    @JsonProperty("cRFO")
    private String cRFO;

    private String parentId;
    private String parentType;
    private String parentName;

    @JsonProperty("cOHFCircuitsesIds")
    private List<String> cOHFCircuitsesIds;
    @JsonProperty("cOHFCircuitsesNames")
    private Map<String, String> cOHFCircuitsesNames;

    @JsonProperty("cEBBMLLsIds")
    private List<String> cEBBMLLsIds;
    @JsonProperty("cEBBMLLsNames")
    private Map<String, String> cEBBMLLsNames;

    @JsonDeserialize(using = FlexibleDateDeserializer.class)
    private LocalDateTime dateStart;
    private LocalDate dateStartDate;

    @JsonDeserialize(using = FlexibleDateDeserializer.class)
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
    private Integer acceptanceTimeMins;

    @JsonProperty("cFieldNotes")
    private String cFieldNotes;
    @JsonProperty("cResolutionNotes")
    private String cResolutionNotes;

    // ── Required when a Field Engineer sets status to "Pending" ──────────────
    private String pendingReason;
}