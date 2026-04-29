package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks current stock of an InventoryItem within a store (area code).
 * One row per (item, storeAreaCode) pair.
 */
@Entity
@Table(name = "inventory_stock",
       uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "store_area_code"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStock {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    /** Area code of the store (matches Team.areaCode) */
    @Column(name = "store_area_code", nullable = false, length = 50)
    private String storeAreaCode;

    @Column(name = "store_name", length = 200)
    private String storeName;

    /** Current quantity on hand */
    @Column(name = "quantity_on_hand", nullable = false)
    private Double quantityOnHand = 0.0;

    /** Low-stock alert threshold */
    @Column(name = "reorder_level")
    private Double reorderLevel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
