package com.cableops.tracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @AllArgsConstructor
public class AccountResponse {
    private String id;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;
}