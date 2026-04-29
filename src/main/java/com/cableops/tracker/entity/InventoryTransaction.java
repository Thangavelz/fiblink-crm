package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit record for every stock movement.
 *
 * txnType values:
 *   INWARD   – stock received into store (manual admin entry)
 *   USAGE    – stock consumed on a task (employee records material used)
 *   ADJUST   – manual admin correction (+/-)
 */
@Entity
@Table(name = "inventory_transactions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {

    @Id
    @Column(length = 36)
    private String id;

    /** INWARD | USAGE | ADJUST */
    @Column(name = "txn_type", nullable = false, length = 20)
    private String txnType;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "store_area_code", nullable = false, length = 50)
    private String storeAreaCode;

    @Column(name = "store_name", length = 200)
    private String storeName;

    /** Positive for INWARD/ADJUST+, negative for USAGE/ADJUST- */
    @Column(name = "quantity_change", nullable = false)
    private Double quantityChange;

    @Column(name = "quantity_after", nullable = false)
    private Double quantityAfter;

    /** Linked task id for USAGE transactions */
    @Column(name = "task_id", length = 36)
    private String taskId;

    @Column(name = "task_name", length = 300)
    private String taskName;

    /** User who performed the action */
    @Column(name = "performed_by_id", length = 36)
    private String performedById;

    @Column(name = "performed_by_name", length = 200)
    private String performedByName;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
