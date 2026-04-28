package com.cableops.tracker.repository;

import com.cableops.tracker.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {
    List<UserTeam> findByUserId(String userId);
    List<UserTeam> findByTeamId(String teamId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserTeam ut WHERE ut.teamId = :teamId")
    void deleteByTeamId(String teamId);
}