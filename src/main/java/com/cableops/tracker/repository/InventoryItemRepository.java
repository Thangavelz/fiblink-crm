package com.cableops.tracker.repository;
 
import com.cableops.tracker.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
 
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    List<InventoryItem> findByItemTypeAndIsActive(String itemType, Boolean isActive);
    List<InventoryItem> findByIsActive(Boolean isActive);
}