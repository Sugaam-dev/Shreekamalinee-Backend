package com.pmrgsolution.features.address.dto;

import com.pmrgsolution.constant.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[0-9+() -]{10,15}$", message = "Please enter a valid phone number")
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
    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be a 6-digit PIN code")
    private String postalCode;

    @Builder.Default
    private String country = "India";

    @Builder.Default
    private AddressType addressType = AddressType.HOME;

    private Boolean isDefault;

    public String getCleanPhoneNumber() {
        return phoneNumber != null ? phoneNumber.replaceAll("\\s+", "").trim() : "";
    }

    public String getCleanPostalCode() {
        return postalCode != null ? postalCode.replaceAll("\\s+", "").trim() : "";
    }
}