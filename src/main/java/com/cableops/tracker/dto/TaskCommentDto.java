package com.cableops.tracker.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class TaskCommentDto {
    private String id;
    private String userId;
    private String userName;
    private String avatarId;
    private String text;
    private String type;   // "comment" | "status" | "update" | "create"
    private String data;   // raw JSON string
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}