package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccountRequest {

    private String id;
    @NotBlank(message = "Name is mandatory")
    private String name;
    private String emailAddress;
    private String phoneNumber;

    private String type;
    private String industry;

    private String billingAddressStreet;
    private String billingAddressCity;
    private String billingAddressState;
    private String billingAddressCountry;
    private String billingAddressPostalCode;

    private String shippingAddressStreet;
    private String shippingAddressCity;
    private String shippingAddressState;
    private String shippingAddressCountry;
    private String shippingAddressPostalCode;

    private String description;
    private String website;

    @JsonProperty("cTelegramgroupchatid")
    private String cTelegramgroupchatid;
}