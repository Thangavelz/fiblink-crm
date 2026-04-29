package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a material item type in the inventory catalogue.
 * e.g. "OFC Cable - SM 6 Core", "JCV Box - Medium"
 */
@Entity
@Table(name = "inventory_items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @Column(length = 36)
    private String id;

    /** "OFC_CABLE" or "JCV_BOX" */
    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    /** Human-readable name: "SM 6 Core", "Medium JCV Box", etc. */
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    /** For OFC: "SM" | "MM" | "ARMORED" etc. Null for JCV boxes */
    @Column(name = "cable_type", length = 50)
    private String cableType;

    /** For OFC: number of fiber cores. Null for JCV boxes */
    @Column(name = "core_count")
    private Integer coreCount;

    /** For JCV Box: "SMALL" | "MEDIUM" | "LARGE". Null for OFC */
    @Column(name = "box_size", length = 50)
    private String boxSize;

    /** Unit of measure: "MTR" for OFC, "PCS" for JCV */
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
