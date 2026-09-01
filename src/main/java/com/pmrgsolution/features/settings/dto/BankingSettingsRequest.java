package com.pmrgsolution.features.settings.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankingSettingsRequest {
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String branchName;
    private String upiId;
    private Boolean isUpiPaymentActive;
    private Boolean isRazorpayPaymentActive;
    private Boolean isCodPaymentActive;
    private Integer estimatedDeliveryDaysMin;
    private Integer estimatedDeliveryDaysMax;
    private Integer returnWindowDays;
    private Boolean isReturnActive;
    private String returnPolicyText;
    private String deliveryPolicyNotice;
    private Boolean isActive;
}