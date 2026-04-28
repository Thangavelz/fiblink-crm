package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task {

	@Id
	private String id;

	private String name;

	private String status;
	private String priority;

	@Column(name = "c_work_type")
	private String cWorkType;

	@Column(name = "c_rfo")
	private String cRFO;

	@Column(name = "c_sr_number")
	private String cSRNumber;

	@Column(name = "parent_id")
	private String parentId;

	@Column(name = "parent_type")
	private String parentType;

	@Column(name = "parent_name")
	private String parentName;

	@Column(name = "date_start")
	private LocalDateTime dateStart;

	@Column(name = "date_start_date")
	private LocalDate dateStartDate;

	@Column(name = "date_completed")
	private LocalDateTime dateCompleted;

	@Column(name = "c_duration_text")
	private String cDurationText;

	@Column(length = 4000)
	private String description;

	@Column(name = "c_note", length = 4000)
	private String cNote;

	@Column(name = "assigned_user_id")
	private String assignedUserId;

	@Column(name = "assigned_user_name")
	private String assignedUserName;

	// ── Multiple secondary users stored as comma-separated ────────────────────
	// MIGRATION: ALTER TABLE tasks
	// ADD COLUMN c_secondary_user_ids VARCHAR(2000),
	// ADD COLUMN c_secondary_user_names VARCHAR(2000);
	//
	// Old single columns kept for backward compat — no longer written to
	@Column(name = "c_secondary_assigned_user_id")
	private String cSecondaryAssignedUserId; // legacy — kept for old data

	@Column(name = "c_secondary_assigned_user_name")
	private String cSecondaryAssignedUserName; // legacy — kept for old data

	@Column(name = "c_secondary_user_ids", length = 2000)
	private String cSecondaryUserIds; // "id1,id2,id3"

	@Column(name = "c_secondary_user_names", length = 2000)
	private String cSecondaryUserNames; // "name1,name2,name3"

	@Column(name = "account_id")
	private String accountId;

	@Column(name = "account_name")
	private String accountName;

	@Column(name = "ofc_type")
	private String ofcType;

	@Column(name = "ofc_starting_mtr")
	private Double ofcStartingMtr;

	@Column(name = "ofc_ending_mtr")
	private Double ofcEndingMtr;

	@Column(name = "fiber_used_mtr")
	private Double fiberUsedMtr;

	@Column(name = "medium_jc_box_used")
	private Integer mediumJcBoxUsed;

	@Column(name = "small_jc_box_used")
	private Integer smallJcBoxUsed;

	@Column(name = "patch_cable_used")
	private Integer patchCableUsed;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;

	@Column(name = "created_by_id")
	private String createdById;

	@Column(name = "created_by_name")
	private String createdByName;

	// Add these fields to your existing Task.java entity

	@Column(name = "acceptance_time_mins")
	private Integer acceptanceTimeMins; // SLA window in minutes

	@Column(name = "accepted_at")
	private LocalDateTime acceptedAt; // when accepted

	@Column(name = "accepted_by_id")
	private String acceptedById;

	@Column(name = "accepted_by_name")
	private String acceptedByName;

	@Column(name = "acceptance_alerted")
	private Boolean acceptanceAlerted = false; // true after overdue alert sent
	
	@Column(name = "c_field_notes", length = 4000)
	private String cFieldNotes;

	@Column(name = "c_resolution_notes", length = 4000)
	private String cResolutionNotes;
}