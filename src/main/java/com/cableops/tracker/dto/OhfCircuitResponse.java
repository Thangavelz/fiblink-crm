package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String circuitId;
    private String circuitStatus;
    private String fiberType;
    private Long   actualLinkDistance;
    private String description;

    private String accountId;
    private String accountName;
    private String contactId;
    private String contactName;

    private List<String>        teamsIds;
    private Map<String, String> teamsNames;
    private List<String>        assignedUsersIds;
    private Map<String, String> assignedUsersNames;
    private List<String>        collaboratorsIds;
    private Map<String, String> collaboratorsNames;

    private String towerAAddressStreet;
    private String towerAAddressCity;
    private String towerAAddressState;
    private String towerAAddressCountry;
    private String towerAAddressPostalCode;
    private String towerALocation;
    private String towerALatLong;

    private String towerBAddressStreet;
    private String towerBAddressCity;
    private String towerBAddressState;
    private String towerBAddressCountry;
    private String towerBAddressPostalCode;
    private String towerBLocation;
    private String towerBLatLong;

    private String customerAddressStreet;
    private String customerAddressCity;
    private String customerAddressState;
    private String customerAddressCountry;
    private String customerAddressPostalCode;
    private String customerlocation;
    private String custometLatLong;

    @JsonProperty("pOPAddressStreet")
    private String pOPAddressStreet;
    @JsonProperty("pOPAddressCity")
    private String pOPAddressCity;
    @JsonProperty("pOPAddressState")
    private String pOPAddressState;
    @JsonProperty("pOPAddressCountry")
    private String pOPAddressCountry;
    @JsonProperty("pOPAddressPostalCode")
    private String pOPAddressPostalCode;
    @JsonProperty("pOPLocation")
    private String pOPLocation;
    @JsonProperty("pOPLatLong")
    private String pOPLatLong;

    @JsonProperty("pONumber")
    private String pONumber;
    @JsonProperty("pODate")
    private LocalDate pODate;

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