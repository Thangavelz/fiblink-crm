package com.cableops.tracker.repository;

import com.cableops.tracker.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, String> {
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
