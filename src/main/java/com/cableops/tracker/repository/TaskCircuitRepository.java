package com.cableops.tracker.repository;

import com.cableops.tracker.entity.TaskCircuit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskCircuitRepository extends JpaRepository<TaskCircuit, Long> {
    List<TaskCircuit> findByTaskId(String taskId);
    void deleteByTaskId(String taskId);
}
