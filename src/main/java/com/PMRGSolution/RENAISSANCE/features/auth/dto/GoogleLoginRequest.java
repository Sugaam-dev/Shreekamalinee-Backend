package com.PMRGSolution.RENAISSANCE.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {
    @NotBlank(message = "Google Token is required")
    private String googleIdToken;

    // These become optional/fallback fields because we will extract 
    // the "Real" values from the token inside the Service.
    private String email;
    private String firstName;
    private String lastName;

    private boolean isMobile; 
}