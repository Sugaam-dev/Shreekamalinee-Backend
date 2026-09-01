package com.pmrgsolution.features.settings.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactSettingsResponse {
    private String whatsappNumber;
    private String supportEmail;
    private String contactAddress;
    private String operatingHours;
}