package com.cableops.tracker.repository;

import com.cableops.tracker.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
	List<Task> findByParentId(String parentId);

	List<Task> findByAssignedUserId(String assignedUserId);

	List<Task> findByStatus(String status);

	List<Task> findByAccountId(String accountId);

	List<Task> findByParentType(String parentType);

	/**
	 * Find all tasks where userId is either the primary assigned user
	 * OR appears in the comma-separated cSecondaryUserIds column.
	 * Used by Field Engineer role to see all their tasks.
	 */
	@Query("SELECT t FROM Task t WHERE " +
	       "t.assignedUserId = :userId OR " +
	       "t.cSecondaryUserIds LIKE CONCAT('%', :userId, '%')")
	List<Task> findByAssignedUserIdOrSecondary(@Param("userId") String userId);

	/**
	 * Find tasks that: - have an acceptance SLA set - have NOT been accepted yet
	 * (acceptedAt is null) - have NOT been alerted yet (acceptanceAlerted is
	 * false/null) - are not already completed or cancelled
	 */
	@Query("SELECT t FROM Task t WHERE " + "t.acceptanceTimeMins IS NOT NULL AND " + "t.acceptedAt IS NULL AND "
			+ "(t.acceptanceAlerted IS NULL OR t.acceptanceAlerted = false) AND "
			+ "t.status NOT IN ('Completed', 'Canceled', 'Accepted', 'In Progress')")
	List<Task> findPendingAcceptanceCheck();
}