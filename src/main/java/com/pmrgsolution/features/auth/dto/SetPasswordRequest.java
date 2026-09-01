package com.pmrgsolution.features.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPasswordRequest {
    @NotBlank(message = "Password is required")
    @com.pmrgsolution.core.security.ValidPassword
    private String newPassword;
}
