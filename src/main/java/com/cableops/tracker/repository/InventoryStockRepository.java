package com.cableops.tracker.repository;
 
import com.cableops.tracker.entity.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
 
public interface InventoryStockRepository extends JpaRepository<InventoryStock, String> {
    List<InventoryStock> findByStoreAreaCode(String storeAreaCode);
    List<InventoryStock> findByItemId(String itemId);
    Optional<InventoryStock> findByItemIdAndStoreAreaCode(String itemId, String storeAreaCode);
}