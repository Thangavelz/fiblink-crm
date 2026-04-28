package com.cableops.tracker.repository;

import com.cableops.tracker.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {
    List<UserTeam> findByUserId(String userId);
    List<UserTeam> findByTeamId(String teamId);
    void deleteByTeamId(String teamId);
}