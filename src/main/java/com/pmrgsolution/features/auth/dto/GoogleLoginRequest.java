package com.pmrgsolution.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    /**
     * The Google ID Token (JWT) obtained from the Google Sign-In client.
     * This token is cryptographically verified on the server side.
     * Passing only an email (without a valid token) is NOT accepted.
     */
    @NotBlank(message = "Google ID Token is required")
    @JsonAlias({"idToken", "token", "credential", "googleToken"})
    private String googleIdToken;

    /** Optional: identifies if request comes from a mobile client for session tracking */
    private boolean isMobile;
}