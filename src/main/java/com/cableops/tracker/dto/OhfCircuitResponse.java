package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class OhfCircuitResponse {

    private String id;
    private String name;
    private String circuitType;
    private String circuitId;       // auto-generated serial
    private String circuitStatus;
    private String fiberType;
    private Long   actualLinkDistance;
    private String description;

    // Relations
    private String accountId;
    private String accountName;
    private String contactId;
    private String contactName;

    // Teams
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
    private String customerlocation;
    private String custometLatLong;       // intentional typo — matches EspoCRM key

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;
}