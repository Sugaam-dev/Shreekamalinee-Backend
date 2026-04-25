package com.PMRGSolution.RENAISSANCE.features.auth.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.PMRGSolution.RENAISSANCE.features.auth.dto.*;
import com.PMRGSolution.RENAISSANCE.features.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Secure endpoints for Identity Management and Session Recovery")
public class AuthController {

    private final AuthService authService;

    /**
     * SESSION RECOVERY (The "Me" Endpoint)
     * Critical for React: Automatically hydrates the AuthContext on refresh.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user details and subscription tier")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        log.info("REST request to recover session for: {}", authentication.getName());
        AuthResponse response = authService.getAuthDetailsByEmail(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegistrationRequest request) {
        log.info("REST request to register user: {}", request.getEmail());
        String message = authService.register(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        log.info("REST request to verify OTP for: {}", request.getEmail());
        AuthResponse response = authService.verifyOtp(request);
        return createResponseWithCookie(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login user: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return createResponseWithCookie(response);
    }

    @PostMapping("/google-authenticate")
    public ResponseEntity<AuthResponse> googleAuthenticate(@Valid @RequestBody GoogleLoginRequest request) {
        // We log that a Google auth request started. 
        // We don't rely on request.getEmail() here because the Service will verify the real email.
        log.info("REST request to authenticate via Google Token");
        
        AuthResponse response = authService.authenticateWithGoogle(request);
        return createResponseWithCookie(response);
    }

    @PostMapping("/complete-google-profile")
    public ResponseEntity<AuthResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        log.info("REST request to complete profile for: {}", request.getEmail());
        AuthResponse response = authService.completeUserProfile(request);
        return createResponseWithCookie(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("REST request to initiate forgot password for: {}", request.getEmail());
        String message = authService.initiateForgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("REST request to reset password for email: {}", request.getEmail());
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/set-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> setPassword(
            @Valid @RequestBody SetPasswordRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("REST request to set password for user: {}", email);
        String message = authService.setPasswordForGoogleUser(email, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * LOGOUT: Securely clears the HTTP-only cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        if (authentication != null) {
            log.info("REST request to logout user: {}", authentication.getName());
            // Since JWT is stateless, we rely on the cookie deletion.
            // If you use ActiveSession database tracking, call authService.logout here.
        }

        ResponseCookie cookie = ResponseCookie.from("renaissance-jwt", "")
                .path("/")
                .maxAge(0) // Expire immediately
                .httpOnly(true)
                .secure(true) // Set to true in production
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    /**
     * PRIVATE HELPER: Handles secure cookie creation and token stripping.
     */
    private ResponseEntity<AuthResponse> createResponseWithCookie(AuthResponse response) {
        String jwt = response.getToken();
        
        // SECURITY: Remove token from the response body so it's only available via Cookie
        response.setToken(null); 

        ResponseCookie cookie = ResponseCookie.from("renaissance-jwt", jwt)
                .path("/")
                .maxAge(24 * 60 * 60) // 24 Hours
                .httpOnly(true)
                .secure(true) // Set to TRUE in production for HTTPS
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}