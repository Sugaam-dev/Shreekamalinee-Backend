package com.pmrgsolution.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 25, message = "Name must be between 2 and 25 characters")
    private String firstName;
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 25, message = "Name must be between 2 and 25 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^\\+?[1-9]\\d{6,14}$",
        message = "Please enter a valid phone number with country code (e.g. +919876543210, +12025550123)"
    )
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @com.pmrgsolution.core.security.ValidPassword
    private String password;
}