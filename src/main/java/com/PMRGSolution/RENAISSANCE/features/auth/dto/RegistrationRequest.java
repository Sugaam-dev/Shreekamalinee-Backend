package com.PMRGSolution.RENAISSANCE.features.auth.dto;

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
        regexp = "^(\\+91)?[6-9][0-9]{9}$",
        message = "Enter a valid Indian phone number"
    )
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}