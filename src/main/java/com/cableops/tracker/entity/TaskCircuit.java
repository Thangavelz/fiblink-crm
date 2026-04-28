package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_circuits")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class TaskCircuit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "circuit_id")
    private String circuitId;

    @Column(name = "circuit_name")
    private String circuitName;
}
