package com.cableops.tracker.dto;

import com.cableops.tracker.config.FlexibleDateDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
	private String cWorkType;
	private String cRFO;

	private String parentId;
	private String parentType;
	private String parentName;

	private List<String>        cOHFCircuitsesIds;
	private Map<String, String> cOHFCircuitsesNames;

	private List<String>        cEBBMLLsIds;
	private Map<String, String> cEBBMLLsNames;

	@JsonDeserialize(using = FlexibleDateDeserializer.class)
	private LocalDateTime dateStart;

	private LocalDate dateStartDate;

	@JsonDeserialize(using = FlexibleDateDeserializer.class)
	private LocalDateTime dateCompleted;

	private String cDurationText;
	private String description;
	private String cNote;

	private String assignedUserId;
	private String assignedUserName;

	private List<String>        cSecondaryAssignedUserIds;
	private Map<String, String> cSecondaryAssignedUserNames;

	private List<String>        teamsIds;
	private Map<String, String> teamsNames;

	private List<String>        attachmentsIds;
	private Map<String, String> attachmentsNames;
	private Map<String, String> attachmentsTypes;

	private String  ofcType;
	private Double  ofcStartingMtr;
	private Double  ofcEndingMtr;
	private Double  fiberUsedMtr;
	private Integer mediumJcBoxUsed;
	private Integer smallJcBoxUsed;
	private Integer patchCableUsed;

	private Integer acceptanceTimeMins;

	private String cFieldNotes;
	private String cResolutionNotes;
}