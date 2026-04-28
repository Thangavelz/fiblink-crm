package com.cableops.tracker.repository;

import com.cableops.tracker.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByParentId(String parentId);
    List<Task> findByAssignedUserId(String assignedUserId);
    List<Task> findByStatus(String status);
    List<Task> findByAccountId(String accountId);
    List<Task> findByParentType(String parentType);

    /**
     * Find tasks that:
     * - have an acceptance SLA set
     * - have NOT been accepted yet (acceptedAt is null)
     * - have NOT been alerted yet (acceptanceAlerted is false/null)
     * - are not already completed or cancelled
     */
    @Query("SELECT t FROM Task t WHERE " +
           "t.acceptanceTimeMins IS NOT NULL AND " +
           "t.acceptedAt IS NULL AND " +
           "(t.acceptanceAlerted IS NULL OR t.acceptanceAlerted = false) AND " +
           "t.status NOT IN ('Completed', 'Canceled')")
    List<Task> findPendingAcceptanceCheck();
}