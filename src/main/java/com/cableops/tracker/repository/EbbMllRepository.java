package com.cableops.tracker.repository;

import com.cableops.tracker.entity.EbbMll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EbbMllRepository extends JpaRepository<EbbMll, String> {
    List<EbbMll> findByAccountId(String accountId);
    List<EbbMll> findByProductType(String productType);
    List<EbbMll> findByAssignedUserId(String assignedUserId);
}
