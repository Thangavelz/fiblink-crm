package com.cableops.tracker.repository;
 
import com.cableops.tracker.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
 
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, String> {
    List<InventoryTransaction> findByStoreAreaCodeOrderByCreatedAtDesc(String storeAreaCode);
    List<InventoryTransaction> findByItemIdOrderByCreatedAtDesc(String itemId);
    List<InventoryTransaction> findByTaskIdOrderByCreatedAtDesc(String taskId);
    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();
    List<InventoryTransaction> findByTxnTypeOrderByCreatedAtDesc(String txnType);
    List<InventoryTransaction> findByPerformedByIdOrderByCreatedAtDesc(String userId);
}