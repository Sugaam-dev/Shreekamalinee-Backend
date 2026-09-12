package com.pmrgsolution.features.auth.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pmrgsolution.constant.AuthProvider;
import com.pmrgsolution.constant.Role;
import com.pmrgsolution.exception.BusinessException;
import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.core.security.JwtUtils;
import com.pmrgsolution.features.auth.dto.*;
import com.pmrgsolution.features.auth.entity.*;
import com.pmrgsolution.features.auth.repository.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.pmrgsolution.features.audit.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Value("${GOOGLE_CLIENT_ID:${spring.security.oauth2.client.registration.google.client-id:google_client_id_placeholder}}")
    private String googleClientId;


    private AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ActiveSessionRepository sessionRepository;
    private final UserEmailOtpRepository emailOtpRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpGenerator otpGenerator;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(
            UserRepository userRepository,
            ActiveSessionRepository sessionRepository,
            UserEmailOtpRepository emailOtpRepository,
            ForgotPasswordRepository forgotPasswordRepository,
            JwtUtils jwtUtils, 
            PasswordEncoder passwordEncoder, 
            EmailService emailService,
            OtpGenerator otpGenerator,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.emailOtpRepository = emailOtpRepository;
        this.forgotPasswordRepository = forgotPasswordRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpGenerator = otpGenerator;
        this.auditLogService = auditLogService;
    }

    @Autowired
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // --- GOOGLE AUTHENTICATION LOGIC (UPDATED) ---

    @Override
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        // SECURITY: Only cryptographic verification is allowed.
        // All bypass paths (dev tokens, email-only) have been removed.
        GoogleIdToken.Payload payload = verifyAndExtractPayload(request.getGoogleIdToken());
        String verifiedEmail   = payload.getEmail().trim().toLowerCase();
        String googleFirstName = (String) payload.get("given_name");
        String googleLastName  = (String) payload.get("family_name");

        // Find or Register the User (Seamless Account Linking)
        User user = userRepository.findByEmailIgnoreCase(verifiedEmail)
                .map(existingUser -> {
                    log.info("Google login for existing user: {}", verifiedEmail);
                    if (!existingUser.isEnabled()) {
                        existingUser.setEnabled(true);
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    log.info("Registering new Google user: {}", verifiedEmail);
                    User newUser = User.builder()
                            .firstName(googleFirstName != null ? googleFirstName : "Google")
                            .lastName(googleLastName  != null ? googleLastName  : "User")
                            .email(verifiedEmail)
                            .role(Role.USER)
                            .enabled(true)
                            .provider(AuthProvider.GOOGLE)
                            .build();
                    return userRepository.save(newUser);
                });

        ActiveSession session = createNewSession(user, "Google Auth", request.isMobile());
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        return buildAuthResponse(user, jwt, session.getRefreshToken());
    }

    /**
     * PRIVATE HELPER: Cryptographically verifies the Google ID Token signature.
     * SECURITY: The unsigned-parse fallback has been intentionally removed to prevent token forgery.
     */
    private GoogleIdToken.Payload verifyAndExtractPayload(String idTokenString) {
        String trimmedClientId = googleClientId != null ? googleClientId.trim() : "";
        log.info("Verifying Google token against Client ID: [{}]", trimmedClientId);

        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BusinessException("Google token is required.", HttpStatus.BAD_REQUEST);
        }

        if (!idTokenString.contains(".") || idTokenString.split("\\.").length != 3) {
            throw new BusinessException("Invalid Google token format.", HttpStatus.BAD_REQUEST);
        }

        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            // SECURITY FIX: Always validate audience — never skip the audience check.
            // Removing the placeholder bypass prevents tokens issued for other Google apps
            // from being accepted here (cross-app token replay attack).
            if (trimmedClientId.isEmpty() || "google_client_id_placeholder".equals(trimmedClientId)) {
                throw new BusinessException("Google authentication is not properly configured on this server.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(transport, jsonFactory);
            verifierBuilder.setAudience(Collections.singletonList(trimmedClientId));
            GoogleIdTokenVerifier verifier = verifierBuilder.build();

            // Cryptographic signature verification — the ONLY accepted path
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BusinessException("Google token verification failed. Token may be expired or forged.", HttpStatus.UNAUTHORIZED);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                log.warn("Rejected Google auth: Email {} is not verified by Google.", payload.getEmail());
                throw new BusinessException("Google email is not verified. Please verify your Google account.", HttpStatus.UNAUTHORIZED);
            }
            return payload;

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Google Token Verification Exception: {}", e.getMessage(), e);
            throw new BusinessException("Google authentication failed. Please try again.", HttpStatus.UNAUTHORIZED);
        }
    }






    // --- EXISTING METHODS ---

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        int tierRank = 0;
        String tierName = "Customer";

        if (Role.SUPERADMIN.equals(user.getRole())) {
            tierRank = 3;
            tierName = "Superadmin";
        } else if (Role.ADMIN.equals(user.getRole())) {
            tierRank = 2;
            tierName = "Admin";
        }

        return AuthResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .phoneNumber(user.getPhoneNumber())
                .isProfileComplete(user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty())
                .tierRank(tierRank)
                .tierName(tierName)
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public String register(RegistrationRequest request) {
        log.info("Registration attempt: {}", request.getEmail());

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber().trim())) {
                throw new BusinessException("This mobile number is already registered with another account. Please login or use a different number.", HttpStatus.CONFLICT);
            }
        }
        
        return userRepository.findByEmailIgnoreCase(request.getEmail())
            .map(user -> {
                if (user.isEnabled()) {
                    throw new BusinessException("Email already verified. Please login.", HttpStatus.CONFLICT);
                }
                generateAndSendRegistrationOtp(user);
                return "User exists but not verified. New OTP sent.";
            })
            .orElseGet(() -> {
                User newUser = User.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .phoneNumber(request.getPhoneNumber())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .role(Role.USER)    
                        .enabled(false) 
                        .provider(AuthProvider.LOCAL)
                        .build();

                userRepository.save(newUser);
                generateAndSendRegistrationOtp(newUser);
                return "Registration successful. Verify your email via OTP.";
            });
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerificationRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found.", HttpStatus.NOT_FOUND));

        UserEmailOtp emailOtp = emailOtpRepository.findByUserAndEmailOtpAndIsUsedFalse(user, hashOtp(request.getOtp()))
                .orElseThrow(() -> new BusinessException("Invalid/Expired OTP.", HttpStatus.BAD_REQUEST));
        
        if (emailOtp.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("OTP expired.", HttpStatus.BAD_REQUEST);
        }

        emailOtp.setUsed(true);
        emailOtp.setVerified(true);
        emailOtpRepository.save(emailOtp);

        user.setEnabled(true); 
        user.resetFailedLogin();
        userRepository.save(user);

        auditLogService.logEvent("EMAIL_VERIFIED", user.getEmail(), null, "User successfully verified email via OTP");

        ActiveSession session = createNewSession(user, "Verification", false);
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        return buildAuthResponse(user, jwt, session.getRefreshToken());
    }
    
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> {
                    auditLogService.logEvent("LOGIN_FAILED", request.getEmail().trim(), null, "Non-existent user attempted login");
                    return new BusinessException("Invalid credentials.", HttpStatus.UNAUTHORIZED);
                });

        // 1. Check if account is locked due to brute force attempts
        if (user.isAccountLocked()) {
            auditLogService.logEvent("ACCOUNT_LOCKED_ATTEMPT", user.getEmail(), null, "Login blocked: Account is locked until " + user.getLockoutUntil());
            throw new BusinessException("Account is temporarily locked due to 5 failed attempts. Please try again after 15 minutes.", HttpStatus.FORBIDDEN);
        }

        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw new BusinessException("Use Google Sign-in for this account.", HttpStatus.BAD_REQUEST);
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword())
            );
            // Reset failed counter on successful authentication
            user.resetFailedLogin();
            userRepository.save(user);
            auditLogService.logEvent("LOGIN_SUCCESS", user.getEmail(), null, "User logged in successfully");
        } catch (org.springframework.security.authentication.DisabledException e) {
            auditLogService.logEvent("LOGIN_FAILED", user.getEmail(), null, "Unverified account attempted login");
            throw new BusinessException("Account not verified. Please verify your OTP.", HttpStatus.FORBIDDEN);
        } catch (AuthenticationException e) {
            // Track failed login attempt
            user.recordFailedLogin();
            userRepository.save(user);
            int remainingAttempts = Math.max(0, 5 - user.getFailedLoginAttempts());
            String message = remainingAttempts > 0 
                    ? "Invalid email or password. " + remainingAttempts + " attempt(s) remaining before temporary lockout."
                    : "Account is temporarily locked for 15 minutes due to repeated failed logins.";
            auditLogService.logEvent(user.isAccountLocked() ? "ACCOUNT_LOCKED" : "LOGIN_FAILED", user.getEmail(), null, "Bad credentials attempt #" + user.getFailedLoginAttempts());
            
            if (user.isAccountLocked()) {
                try {
                    emailService.sendAccountLockedAlert(user.getEmail(), user.getFirstName());
                } catch (Exception mailEx) {
                    log.error("Failed to send account locked alert email to {}: {}", user.getEmail(), mailEx.getMessage());
                }
            }

            throw new BusinessException(message, HttpStatus.UNAUTHORIZED);
        }

        ActiveSession session = createNewSession(user, "Web", false);
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        return buildAuthResponse(user, jwt, session.getRefreshToken());
    }

    @Override
    public AuthResponse getAuthDetailsByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Session expired."));
        return buildAuthResponse(user, null, null);
    }

    @Override
    @Transactional
    public AuthResponse completeUserProfile(CompleteProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found.", HttpStatus.NOT_FOUND));

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String newPhone = request.getPhoneNumber().trim();
            if (!newPhone.equals(user.getPhoneNumber())) {
                if (userRepository.existsByPhoneNumber(newPhone)) {
                    throw new BusinessException("This mobile number is already linked to another account. Please use a different number.", HttpStatus.CONFLICT);
                }
                user.setPhoneNumber(newPhone);
            }
        }
        userRepository.save(user);
        
        ActiveSession session = createNewSession(user, "Profile Sync", false);
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        return buildAuthResponse(user, jwt, session.getRefreshToken());
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(String requestRefreshToken) {
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            throw new BusinessException("Refresh token is missing", HttpStatus.BAD_REQUEST);
        }

        if (!jwtUtils.validateJwtToken(requestRefreshToken)) {
            throw new BusinessException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        Optional<ActiveSession> sessionOpt = sessionRepository.findByRefreshToken(requestRefreshToken);

        if (sessionOpt.isEmpty()) {
            // Grace Period Check: Allow recently rotated token within 30 seconds to handle concurrent frontend requests
            Optional<ActiveSession> graceSessionOpt = sessionRepository.findByPreviousRefreshToken(requestRefreshToken);
            if (graceSessionOpt.isPresent()) {
                ActiveSession graceSession = graceSessionOpt.get();
                if (graceSession.getPreviousRefreshTokenExpiry() != null &&
                        graceSession.getPreviousRefreshTokenExpiry().isAfter(LocalDateTime.now())) {
                    User user = graceSession.getUser();
                    if (!user.isEnabled() || user.isAccountLocked()) {
                        sessionRepository.delete(graceSession);
                        throw new BusinessException("Account is inactive or locked.", HttpStatus.FORBIDDEN);
                    }

                    log.debug("Refresh token grace period matched for session: {}", graceSession.getSessionId());
                    String newAccessToken = jwtUtils.generateToken(user.getEmail(), graceSession.getSessionId().toString());
                    return TokenRefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(graceSession.getRefreshToken())
                            .tokenType("Bearer")
                            .build();
                }
            }

            throw new BusinessException("Invalid or revoked refresh session. Please login again.", HttpStatus.UNAUTHORIZED);
        }

        ActiveSession session = sessionOpt.get();

        if (session.getRefreshTokenExpiry() != null && session.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session);
            throw new BusinessException("Refresh token has expired. Please login again.", HttpStatus.UNAUTHORIZED);
        }

        User user = session.getUser();
        if (!user.isEnabled() || user.isAccountLocked()) {
            sessionRepository.delete(session);
            throw new BusinessException("Account is inactive or locked.", HttpStatus.FORBIDDEN);
        }

        // Automatic Token Rotation with 30-second Grace Window for In-Flight Concurrent Requests
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getEmail(), session.getSessionId().toString());
        session.setPreviousRefreshToken(session.getRefreshToken());
        session.setPreviousRefreshTokenExpiry(LocalDateTime.now().plusSeconds(30));
        session.setRefreshToken(newRefreshToken);
        session.setRefreshTokenExpiry(LocalDateTime.now().plusNanos(jwtUtils.getRefreshExpirationMs() * 1_000_000L));
        session.setLastActive(LocalDateTime.now());
        sessionRepository.save(session);

        String newAccessToken = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());

        auditLogService.logEvent("TOKEN_ROTATED", user.getEmail(), null, "Access token refreshed with automatic token rotation");

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .build();
    }

    private ActiveSession createNewSession(User user, String deviceId, boolean isMobile) {
        sessionRepository.deleteByUser(user); 

        UUID sessionId = UUID.randomUUID();
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), sessionId.toString());
        LocalDateTime refreshTokenExpiry = LocalDateTime.now().plusNanos(jwtUtils.getRefreshExpirationMs() * 1_000_000L);

        ActiveSession session = ActiveSession.builder()
                .user(user)
                .sessionId(sessionId)
                .refreshToken(refreshToken)
                .refreshTokenExpiry(refreshTokenExpiry)
                .deviceId(deviceId)
                .deviceType(isMobile)
                .loginTime(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .build();
                
        return sessionRepository.save(session);
    }

    private void generateAndSendRegistrationOtp(User user) {
        String otp = otpGenerator.generateOtp();
        UserEmailOtp emailOtp = UserEmailOtp.builder()
                .user(user)
                .emailOtp(hashOtp(otp))
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .isUsed(false)
                .isVerified(false)
                .createdAt(OffsetDateTime.now())
                .build();
        emailOtpRepository.save(emailOtp);
        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    @Override
    @Transactional
    public String initiateForgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String otp = otpGenerator.generateOtp();
            ForgotPassword fp = ForgotPassword.builder()
                    .user(user)
                    .otp(hashOtp(otp))
                    .isUsed(false)
                    .expiresAt(OffsetDateTime.now().plusMinutes(10))
                    .createdAt(OffsetDateTime.now())
                    .build();
            
            forgotPasswordRepository.save(fp);
            emailService.sendPasswordResetEmail(user.getEmail(), otp);
        });
        return "Reset OTP sent if account exists.";
    }

    @Override
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BusinessException("Invalid request.", HttpStatus.BAD_REQUEST));

        ForgotPassword fp = forgotPasswordRepository.findByUserAndOtpAndIsUsedFalse(user, hashOtp(request.getOtp()))
                .orElseThrow(() -> new BusinessException("Invalid/Expired OTP.", HttpStatus.BAD_REQUEST));

        // SECURITY FIX: Validate OTP expiry — matches the same check in verifyOtp().
        // Previously, expired password reset OTPs could still be used indefinitely.
        if (fp.getExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new BusinessException("OTP has expired. Please request a new password reset.", HttpStatus.BAD_REQUEST);
        }

        fp.setUsed(true);
        forgotPasswordRepository.save(fp);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.resetFailedLogin();
        userRepository.save(user);

        auditLogService.logEvent("PASSWORD_RESET", user.getEmail(), null, "Password reset successfully via OTP verification");

        sessionRepository.deleteByUser(user); 

        try {
            emailService.sendPasswordChangedAlert(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send password changed alert email to {}: {}", user.getEmail(), e.getMessage());
        }

        return "Password updated successfully.";
    }

    @Override
    @Transactional
    public String setPasswordForGoogleUser(String email, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        try {
            emailService.sendPasswordChangedAlert(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send password set alert email to {}: {}", user.getEmail(), e.getMessage());
        }

        return "Password set.";
    }

    @Override
    @Transactional 
    public void logout(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        try {
            UUID id = UUID.fromString(sessionId);
            sessionRepository.findBySessionId(id).ifPresent(sessionRepository::delete);
        } catch (Exception e) {
            log.warn("Invalid session ID format: {}", sessionId);
        }
    }

    private String hashOtp(String otp) {
        if (otp == null) return null;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}