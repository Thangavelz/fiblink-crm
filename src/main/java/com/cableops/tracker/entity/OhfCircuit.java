package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ohf_circuits")
@Getter @Setter
public class OhfCircuit {

    @Id
    private String id;

    // ── Overview ──────────────────────────────────────────────────────────────
    @Column(nullable = false)
    private String name;

    @Column(name = "circuit_type")
    private String circuitType;   // Infra / OHF / UG / MW / Ent Customer

    @Column(name = "circuit_id")
    private String circuitId;     // auto-generated serial e.g. "20250194"

    @Column(name = "circuit_status")
    private String circuitStatus; // Active / Under Provisioning / Pending/Hold / Suspended / Acceptance Pending

    @Column(name = "fiber_type")
    private String fiberType;     // Single Core / Multi Core

    @Column(name = "actual_link_distance")
    private Long actualLinkDistance; // metres

    @Column(length = 2000)
    private String description;

    // ── Relations ─────────────────────────────────────────────────────────────
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "contact_id")
    private String contactId;

    @Column(name = "contact_name")
    private String contactName;

    // ── Tower A (used by Infra / OHF / UG / MW) ───────────────────────────────
    @Column(name = "tower_a_address_street")
    private String towerAAddressStreet;

    @Column(name = "tower_a_address_city")
    private String towerAAddressCity;

    @Column(name = "tower_a_address_state")
    private String towerAAddressState;

    @Column(name = "tower_a_address_country")
    private String towerAAddressCountry;

    @Column(name = "tower_a_address_postal_code")
    private String towerAAddressPostalCode;

    @Column(name = "tower_a_location", length = 500)
    private String towerALocation;

    @Column(name = "tower_a_lat_long")
    private String towerALatLong;

    // ── Tower B (used by Infra / OHF / UG / MW) ───────────────────────────────
    @Column(name = "tower_b_address_street")
    private String towerBAddressStreet;

    @Column(name = "tower_b_address_city")
    private String towerBAddressCity;

    @Column(name = "tower_b_address_state")
    private String towerBAddressState;

    @Column(name = "tower_b_address_country")
    private String towerBAddressCountry;

    @Column(name = "tower_b_address_postal_code")
    private String towerBAddressPostalCode;

    @Column(name = "tower_b_location", length = 500)
    private String towerBLocation;

    @Column(name = "tower_b_lat_long")
    private String towerBLatLong;

    // ── Customer Address (used by Ent Customer) ────────────────────────────────
    @Column(name = "customer_address_street")
    private String customerAddressStreet;

    @Column(name = "customer_address_city")
    private String customerAddressCity;

    @Column(name = "customer_address_state")
    private String customerAddressState;

    @Column(name = "customer_address_country")
    private String customerAddressCountry;

    @Column(name = "customer_address_postal_code")
    private String customerAddressPostalCode;

    @Column(name = "customer_location", length = 500)
    private String customerlocation;

    @Column(name = "customer_lat_long")
    private String custometLatLong;

    // ── POP Address (used by Ent Customer) ────────────────────────────────────
    @Column(name = "pop_address_street")
    private String pOPAddressStreet;

    @Column(name = "pop_address_city")
    private String pOPAddressCity;

    @Column(name = "pop_address_state")
    private String pOPAddressState;

    @Column(name = "pop_address_country")
    private String pOPAddressCountry;

    @Column(name = "pop_address_postal_code")
    private String pOPAddressPostalCode;

    @Column(name = "pop_location", length = 500)
    private String pOPLocation;

    @Column(name = "pop_lat_long")
    private String pOPLatLong;

    // ── Commercial Details ────────────────────────────────────────────────────
    @Column(name = "po_number")
    private String pONumber;

    @Column(name = "po_date")
    private LocalDate pODate;

    @Column(name = "po_distance_in_km", precision = 10, scale = 2)
    private BigDecimal podistanceinkm;

    @Column(name = "acceptance_date")
    private LocalDate acceptanceDate;

    @Column(name = "one_time_charges", precision = 15, scale = 2)
    private BigDecimal oneTimeCharges;

    @Column(name = "one_time_charges_currency", length = 10)
    private String oneTimeChargesCurrency;

    @Column(name = "arc_amount", precision = 15, scale = 2)
    private BigDecimal arcAmount;

    @Column(name = "arc_as_per_rate_card_per_km", precision = 15, scale = 2)
    private BigDecimal arcasperRatecardperKm;

    @Column(name = "arc_as_per_rate_card_per_km_currency", length = 10)
    private String arcasperRatecardperKmCurrency;

    @Column(name = "amount_per_km_per_month", precision = 15, scale = 2)
    private BigDecimal amountPerKmPerMonth;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
    
    @Column(name = "suspended_date")
    private LocalDate suspendedDate;

    @Column(name = "suspended_reason", length = 1000)
    private String suspendedReason;
}