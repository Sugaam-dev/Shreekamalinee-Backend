package com.pmrgsolution.features.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CompleteProfileRequest {
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Please enter a valid phone number with country code (7-15 digits)")
    private String phoneNumber;
}