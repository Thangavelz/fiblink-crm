package com.cableops.tracker.dto;

import lombok.Data;

@Data
public class TaskCommentRequest {
    private String text;
    private String userId;
    private String userName;
    private String avatarId;
}
