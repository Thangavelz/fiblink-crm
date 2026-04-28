package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_ebb_mlls")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class TaskEbbMll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "ebb_mll_id")
    private String ebbMllId;

    @Column(name = "ebb_mll_name")
    private String ebbMllName;
}