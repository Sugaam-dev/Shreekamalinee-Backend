package com.pmrgsolution.features.settings.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettingsResponse {
    private UUID id;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String branchName;
    private String upiId;
    private String qrCodeUrl;
    private String whatsappNumber;
    private String supportEmail;
    private BigDecimal freeShippingThreshold;
    private BigDecimal standardShippingFee;
    private BigDecimal codHandlingFee;
    private Boolean isFreeShippingPromoActive;
    private Boolean isAnnouncementActive;
    private String announcementText;
    private String announcementsJson;
    private String announcementLink;

    // Payment methods toggles
    private Boolean isUpiPaymentActive;
    private Boolean isRazorpayPaymentActive;
    private Boolean isCodPaymentActive;

    // Delivery SLA & Return Policy
    private Integer estimatedDeliveryDaysMin;
    private Integer estimatedDeliveryDaysMax;
    private Integer returnWindowDays;
    private Boolean isReturnActive;
    private String returnPolicyText;
    private String deliveryPolicyNotice;

    // Contact & Studio
    private String contactAddress;
    private String operatingHours;
    private String contactPhone;
    private String contactEmail;

    private Boolean isActive;
}