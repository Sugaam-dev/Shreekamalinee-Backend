package com.pmrgsolution.features.settings.dto;

import lombok.*;

/**
 * Restricted public DTO for the bank-details / UPI payment endpoint.
 *
 * SECURITY FIX: Only payment-relevant fields are exposed.
 * Previously the full StoreSettingsResponse was returned, leaking
 * internal shipping fees, COD charges, promo flags, and config data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDetailsResponse {
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String branchName;
    private String upiId;
    private String qrCodeUrl;
    private String whatsappNumber;
}
