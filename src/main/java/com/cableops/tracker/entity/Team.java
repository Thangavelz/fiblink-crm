package com.cableops.tracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "teams")
@Data
public class Team {

    @Id                          // ✅ FIXED: was org.springframework.data.annotation.Id (wrong!)
    private String id;

    private String name;

    @Column(name = "area_code")
    private String areaCode;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}