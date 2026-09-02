package com.pmrgsolution.features.settings.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingSettingsRequest {
    private BigDecimal freeShippingThreshold;
    private BigDecimal standardShippingFee;
    private BigDecimal codHandlingFee;
    private BigDecimal freeCodThreshold;
    private Boolean isFreeShippingPromoActive;
    private Integer estimatedDeliveryDaysMin;
    private Integer estimatedDeliveryDaysMax;
    private Integer returnWindowDays;
    private Boolean isReturnActive;
    private String returnPolicyText;
    private String deliveryPolicyNotice;
}