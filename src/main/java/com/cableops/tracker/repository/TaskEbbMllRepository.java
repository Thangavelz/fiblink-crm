package com.cableops.tracker.repository;

import com.cableops.tracker.entity.TaskEbbMll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskEbbMllRepository extends JpaRepository<TaskEbbMll, Long> {
    List<TaskEbbMll> findByTaskId(String taskId);
    void deleteByTaskId(String taskId);
}