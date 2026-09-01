package com.pmrgsolution.features.settings.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankingSettingsResponse {
    private UUID id;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String branchName;
    private String upiId;
    private String qrCodeUrl;
    private Boolean isActive;
}