package com.cableops.tracker.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class OhfCircuitRequest {

    // Overview
    private String name;
    private String circuitType;       // Infra / OHF / UG / MW / Ent Customer
    private String circuitStatus;     // Active / Under Provisioning / Pending/Hold / Suspended / Acceptance Pending
    private String fiberType;         // Single Core / Multi Core
    private Long   actualLinkDistance;
    private String description;

    // Relations
    private String accountId;
    private String accountName;
    private String contactId;
    private String contactName;

    // Teams (stored in circuit_teams join table)
    private List<String>        teamsIds;
    private Map<String, String> teamsNames;

    // Assigned users
    private List<String>        assignedUsersIds;
    private Map<String, String> assignedUsersNames;

    // Collaborators
    private List<String>        collaboratorsIds;
    private Map<String, String> collaboratorsNames;

    // ── Tower A (Infra / OHF / UG / MW) ──────────────────────────────────────
    private String towerAAddressStreet;
    private String towerAAddressCity;
    private String towerAAddressState;
    private String towerAAddressCountry;
    private String towerAAddressPostalCode;
    private String towerALocation;
    private String towerALatLong;

    // ── Tower B (Infra / OHF / UG / MW) ──────────────────────────────────────
    private String towerBAddressStreet;
    private String towerBAddressCity;
    private String towerBAddressState;
    private String towerBAddressCountry;
    private String towerBAddressPostalCode;
    private String towerBLocation;
    private String towerBLatLong;

    // ── Customer Address (Ent Customer) ───────────────────────────────────────
    private String customerAddressStreet;
    private String customerAddressCity;
    private String customerAddressState;
    private String customerAddressCountry;
    private String customerAddressPostalCode;
    private String customerlocation;      // exact key EspoCRM sends
    private String custometLatLong;       // exact key EspoCRM sends (typo intentional — matches Espo)

    // ── POP Address (Ent Customer) ────────────────────────────────────────────
    private String pOPAddressStreet;
    private String pOPAddressCity;
    private String pOPAddressState;
    private String pOPAddressCountry;
    private String pOPAddressPostalCode;
    private String pOPLocation;
    private String pOPLatLong;

    // Commercial
    private String     pONumber;
    private LocalDate  pODate;
    private BigDecimal podistanceinkm;
    private LocalDate  acceptanceDate;
    private BigDecimal oneTimeCharges;
    private String     oneTimeChargesCurrency;
    private BigDecimal arcAmount;
    private BigDecimal arcasperRatecardperKm;
    private String     arcasperRatecardperKmCurrency;
    private BigDecimal amountPerKmPerMonth;

    // EspoCRM sends this but we ignore it (multiEnum is always [])
    private List<String> multiEnum;
}