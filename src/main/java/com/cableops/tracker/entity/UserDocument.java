package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_documents")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDocument {

    @Id
    private String id;

    private String userId;
    private String fileName;
    private String fileType;
}