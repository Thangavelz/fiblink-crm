package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
	private String cWorkType;
	private String cRFO;
	private String cSRNumber;

	private String parentId;
	private String parentType;
	private String parentName;

	private List<String> cOHFCircuitsesIds;
	private Map<String, String> cOHFCircuitsesNames;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateStart;
	private LocalDate dateStartDate;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateCompleted;

	private String cDurationText;
	private String description;
	private String cNote;

	private String assignedUserId;
	private String assignedUserName;

	private List<String> cSecondaryAssignedUserIds;
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

	private List<String> cEBBMLLsIds;
	private Map<String, String> cEBBMLLsNames;

	private String cFieldNotes;
	private String cResolutionNotes;

}