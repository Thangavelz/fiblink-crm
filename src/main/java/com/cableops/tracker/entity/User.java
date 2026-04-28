package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    private String id;

    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;

    private String password;

    @Column(name = "salutation_name")
    private String salutationName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String name;

    private String type;

    @Column(name = "is_active")
    private Boolean isActive;

    private String title;
    private String gender;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "aadhar_number")
    private String aadharNumber;

    @Column(name = "emergency_contact_number")
    private String emergencyContactNumber;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "avatar_id")
    private String avatarId;

    @Column(name = "avatar_name")
    private String avatarName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}