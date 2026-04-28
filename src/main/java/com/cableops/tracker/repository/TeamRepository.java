package com.cableops.tracker.repository;

import com.cableops.tracker.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, String> {
    boolean existsByNameIgnoreCase(String name);
}