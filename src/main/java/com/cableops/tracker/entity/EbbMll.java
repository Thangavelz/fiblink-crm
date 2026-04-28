package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ebb_mll")
@Getter @Setter
public class EbbMll {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    // Account relation
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "account_name")
    private String accountName;

    // Address
    @Column(name = "address_street")
    private String addressStreet;

    @Column(name = "address_city")
    private String addressCity;

    @Column(name = "address_state")
    private String addressState;

    @Column(name = "address_postal_code")
    private String addressPostalCode;

    @Column(name = "address_country")
    private String addressCountry;

    // Product Type: EBB or MLL
    @Column(name = "product_type")
    private String productType;

    // Customer details
    @Column(name = "customer_location", length = 500)
    private String customerLocation;

    @Column(name = "customer_lat_long")
    private String customerLatLong;

    @Column(name = "spoc_lc_details", length = 500)
    private String spocLcDetails;

    @Column(length = 2000)
    private String description;

    // Assigned user (optional)
    @Column(name = "assigned_user_id")
    private String assignedUserId;

    @Column(name = "assigned_user_name")
    private String assignedUserName;

    // Teams (comma-separated IDs and names)
    @Column(name = "team_ids", length = 1000)
    private String teamIds;

    @Column(name = "team_names", length = 1000)
    private String teamNames;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "created_by_id")
    private String createdById;

    @Column(name = "created_by_name")
    private String createdByName;
}
