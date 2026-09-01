package com.pmrgsolution.features.address.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String alternatePhone;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    private String country;
    private String addressType;
    private Boolean isDefault;

    public String getCleanPhoneNumber() {
        if (phoneNumber == null) return "";
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits.substring(2);
        }
        return digits.length() >= 10 ? digits.substring(digits.length() - 10) : digits;
    }

    public String getCleanPostalCode() {
        if (postalCode == null) return "";
        return postalCode.replaceAll("[^0-9]", "").trim();
    }
}