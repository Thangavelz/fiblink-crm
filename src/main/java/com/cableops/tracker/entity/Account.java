package com.cableops.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter @Setter
public class Account {

    @Id
    private String id; // Espo id or UUID

    @Column(nullable = false)
    private String name;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String type;
    private String industry;

    // Billing
    @Column(name = "billing_address_street")
    private String billingAddressStreet;

    @Column(name = "billing_address_city")
    private String billingAddressCity;

    @Column(name = "billing_address_state")
    private String billingAddressState;

    @Column(name = "billing_address_country")
    private String billingAddressCountry;

    @Column(name = "billing_address_postal_code")
    private String billingAddressPostalCode;

    // Shipping
    @Column(name = "shipping_address_street")
    private String shippingAddressStreet;

    @Column(name = "shipping_address_city")
    private String shippingAddressCity;

    @Column(name = "shipping_address_state")
    private String shippingAddressState;

    @Column(name = "shipping_address_country")
    private String shippingAddressCountry;

    @Column(name = "shipping_address_postal_code")
    private String shippingAddressPostalCode;

    @Column(length = 2000)
    private String description;

    @Column(name = "telegram_group_chat_id")
    private String cTelegramgroupchatid;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "website")
    private String website;
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}