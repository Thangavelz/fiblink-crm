package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_teams")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class UserTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String teamId;

    @Column(name = "team_name")
    private String teamName;   // ✅ stored so GET/LIST can return teamsNames map without a JOIN
}