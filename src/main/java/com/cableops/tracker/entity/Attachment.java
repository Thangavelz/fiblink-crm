package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Data
public class Attachment {

    @Id   // ✅ THIS IS MISSING
    private String id;

    private String name;
    private String type;
    private Long size;

    private String path;

    private String relatedType;
    private String relatedId;

    private String field;

    private LocalDateTime createdAt;
}