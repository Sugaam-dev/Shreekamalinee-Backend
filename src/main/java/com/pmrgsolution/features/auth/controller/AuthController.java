package com.pmrgsolution.features.auth.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pmrgsolution.features.auth.dto.*;
import com.pmrgsolution.features.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Secure endpoints for Identity Management and Session Recovery")
public class AuthController {

    private final AuthService authService;
    private final com.pmrgsolution.features.auth.service.EmailUsageService emailUsageService;

    @org.springframework.beans.factory.annotation.Value("${shreekamalinee.jwt.refreshExpirationMs:7776000000}")
    private long refreshExpirationMs;

    @org.springframework.beans.factory.annotation.Value("${shreekamalinee.cookie.secure:false}")
    private boolean cookieSecure;

    @org.springframework.beans.factory.annotation.Value("${shreekamalinee.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * SESSION RECOVERY (The "Me" Endpoint)
     * Critical for React: Automatically hydrates the AuthContext on refresh.
     */
    @GetMapping({"", "/me"})
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
        log.info("REST request for Google Authentication");
        AuthResponse response = authService.authenticateWithGoogle(request);
        return createResponseWithCookie(response);
    }

    @PostMapping("/complete-google-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        log.info("REST request to complete profile for: {}", request.getEmail());
        AuthResponse response = authService.completeUserProfile(request);
        return createResponseWithCookie(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("REST request for forgot-password: {}", request.getEmail());
        String message = authService.initiateForgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("REST request for reset-password: {}", request.getEmail());
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/set-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> setPasswordForGoogle(
            Authentication authentication,
            @Valid @RequestBody SetPasswordRequest request) {
        log.info("REST request to set password for OAuth user: {}", authentication.getName());
        String message = authService.setPasswordForGoogleUser(authentication.getName(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * REFRESH TOKEN ROTATION:
     * Dual-Mode support: Automatically reads from the secure HTTP-Only cookie,
     * or seamlessly falls back to the JSON payload for mobile/native apps.
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token using a valid rotating refresh token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @CookieValue(name = "shreekamalinee-refresh", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest bodyRequest) {

        String tokenToUse = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (bodyRequest != null ? bodyRequest.getRefreshToken() : null);

        if (tokenToUse == null || tokenToUse.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenRefreshResponse response = authService.refreshToken(tokenToUse);

        ResponseCookie accessCookie = ResponseCookie.from("shreekamalinee-jwt", response.getAccessToken())
                .path("/")
                .maxAge(15 * 60) // 15 Minutes
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();

        ResponseCookie refreshCookieObj = ResponseCookie.from("shreekamalinee-refresh", response.getRefreshToken())
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookieObj.toString())
                .body(response);
    }

    /**
     * LOGOUT: Securely clears the HTTP-only cookies and invalidates session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        if (authentication != null) {
            log.info("REST request to logout user: {}", authentication.getName());
        }

        ResponseCookie accessCookie = ResponseCookie.from("shreekamalinee-jwt", "")
                .path("/")
                .maxAge(0) // Expire immediately
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("shreekamalinee-refresh", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    /**
     * PRIVATE HELPER: Handles secure HttpOnly cookie creation for access and refresh tokens.
     */
    private ResponseEntity<AuthResponse> createResponseWithCookie(AuthResponse response) {
        String jwt = response.getToken();
        String refreshToken = response.getRefreshToken();

        ResponseCookie accessCookie = ResponseCookie.from("shreekamalinee-jwt", jwt != null ? jwt : "")
                .path("/")
                .maxAge(15 * 60) // 15 Minutes for Access Token
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("shreekamalinee-refresh", refreshToken != null ? refreshToken : "")
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    /**
     * PUBLIC EMAIL SERVICE & QUOTA STATUS
     * Used by the frontend to detect if Resend daily quota is exhausted
     * and recommend Google Sign-in to users.
     */
    @GetMapping("/email-service-status")
    @Operation(summary = "Check if transactional email verification is currently active or quota exceeded")
    public ResponseEntity<PublicEmailStatusResponse> getEmailServiceStatus() {
        return ResponseEntity.ok(emailUsageService.getPublicEmailStatus());
    }
}