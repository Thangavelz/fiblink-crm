package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_comments")
@Getter @Setter
public class TaskComment {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "avatar_id")
    private String avatarId;

    @Column(length = 4000)
    private String text;

    /** "comment" | "status" | "update" | "create" */
    @Column(name = "type", length = 20)
    private String type;

    /** JSON payload — e.g. {"status":"Completed"} or {"fields":["RFO","Fiber Used (Mtr)"]} */
    @Column(name = "data", length = 2000)
    private String data;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}