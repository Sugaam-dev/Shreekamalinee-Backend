package com.PMRGSolution.RENAISSANCE.features.auth.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
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

import com.PMRGSolution.RENAISSANCE.Constant.AuthProvider;
import com.PMRGSolution.RENAISSANCE.Constant.Role;
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.Exception.BusinessException;
import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import com.PMRGSolution.RENAISSANCE.core.security.JwtUtils;
import com.PMRGSolution.RENAISSANCE.features.auth.dto.*;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.*;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.*;
import com.PMRGSolution.RENAISSANCE.features.payment.repository.UserSubscriptionRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ActiveSessionRepository sessionRepository;
    private final UserEmailOtpRepository emailOtpRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpGenerator otpGenerator;

    public AuthServiceImpl(
            UserRepository userRepository,
            ActiveSessionRepository sessionRepository,
            UserEmailOtpRepository emailOtpRepository,
            ForgotPasswordRepository forgotPasswordRepository,
            UserSubscriptionRepository subscriptionRepository,
            JwtUtils jwtUtils, 
            PasswordEncoder passwordEncoder, 
            EmailService emailService,
            OtpGenerator otpGenerator) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.emailOtpRepository = emailOtpRepository;
        this.forgotPasswordRepository = forgotPasswordRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpGenerator = otpGenerator;
    }

    @Autowired
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // --- GOOGLE AUTHENTICATION LOGIC (UPDATED) ---

    @Override
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        // 1. Securely extract data from the Token (Source of Truth)
        GoogleIdToken.Payload payload = verifyAndExtractPayload(request.getGoogleIdToken());
        
        String verifiedEmail = payload.getEmail();
        String googleFirstName = (String) payload.get("given_name");
        String googleLastName = (String) payload.get("family_name");

        // 2. Find or Register the User
        User user = userRepository.findByEmailIgnoreCase(verifiedEmail)
                .orElseGet(() -> {
                    log.info("Registering new Google user: {}", verifiedEmail);
                    User newUser = User.builder()
                            .firstName(googleFirstName != null ? googleFirstName : "Google")
                            .lastName(googleLastName != null ? googleLastName : "User")
                            .email(verifiedEmail)
                            .role(Role.USER)
                            .enabled(true) // Google users are pre-verified
                            .provider(AuthProvider.GOOGLE)
                            .build();
                    return userRepository.save(newUser);
                });

        // 3. Prevent cross-provider login
        if (user.getProvider() == AuthProvider.LOCAL) {
            throw new BusinessException("This account is registered with a password. Please use standard login.", HttpStatus.BAD_REQUEST);
        }

        // 4. Create Session and JWT
        ActiveSession session = createNewSession(user, "Google Auth", request.isMobile());
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        
        return buildAuthResponse(user, jwt);
    }

    /**
     * PRIVATE HELPER: Verifies the integrity of the Google ID Token and returns the Payload.
     */
    private GoogleIdToken.Payload verifyAndExtractPayload(String idTokenString) {
    	log.info("Verifying token against Client ID: {}", googleClientId);
        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
        } catch (Exception e) {
            log.error("Google Token Verification Failed: {}", e.getMessage());
        }
        throw new BusinessException("Invalid or Expired Google Token.", HttpStatus.UNAUTHORIZED);
    }

    // --- EXISTING METHODS ---

    private AuthResponse buildAuthResponse(User user, String token) {
        int maxRank = subscriptionRepository.findAllActiveSubscriptions(user.getEmail(), LocalDateTime.now())
                .stream()
                .map(sub -> sub.getTier().getRank())
                .max(Integer::compare)
                .orElse(0);

        String tierName = "Free";
        for (TierType type : TierType.values()) {
            if (type.getRank() == maxRank) {
                tierName = formatName(type.name());
                break;
            }
        }

        return AuthResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .phoneNumber(user.getPhoneNumber())
                .isProfileComplete(user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty())
                .tierRank(maxRank)
                .tierName(tierName)
                .token(token)
                .build();
    }

    private String formatName(String name) {
        String n = name.toLowerCase().replace("_", " ");
        return n.substring(0, 1).toUpperCase() + n.substring(1);
    }

    @Override
    @Transactional
    public String register(RegistrationRequest request) {
        log.info("Registration attempt: {}", request.getEmail());
        
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

        UserEmailOtp emailOtp = emailOtpRepository.findByUserAndEmailOtpAndIsUsedFalse(user, request.getOtp())
                .orElseThrow(() -> new BusinessException("Invalid/Expired OTP.", HttpStatus.BAD_REQUEST));
        
        if (emailOtp.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("OTP expired.", HttpStatus.BAD_REQUEST);
        }

        emailOtp.setUsed(true);
        emailOtp.setVerified(true);
        emailOtpRepository.save(emailOtp);

        user.setEnabled(true); 
        userRepository.save(user);

        ActiveSession session = createNewSession(user, "Verification", false);
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        return buildAuthResponse(user, jwt);
    }
    
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BusinessException("Invalid credentials.", HttpStatus.UNAUTHORIZED));

        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw new BusinessException("Use Google Sign-in for this account.", HttpStatus.BAD_REQUEST);
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new BusinessException("Account not verified. Please verify your OTP.", HttpStatus.FORBIDDEN);
        } catch (AuthenticationException e) {
            throw new BusinessException("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        }

        ActiveSession session = createNewSession(user, "Web", false);
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        return buildAuthResponse(user, jwt);
    }

    @Override
    public AuthResponse getAuthDetailsByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Session expired."));
        return buildAuthResponse(user, null);
    }

    @Override
    @Transactional
    public AuthResponse completeUserProfile(CompleteProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found.", HttpStatus.NOT_FOUND));

        user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);
        
        ActiveSession session = createNewSession(user, "Profile Sync", false);
        String jwt = jwtUtils.generateToken(user.getEmail(), session.getSessionId().toString());
        return buildAuthResponse(user, jwt);
    }

    private ActiveSession createNewSession(User user, String deviceId, boolean isMobile) {
        sessionRepository.deleteByUser(user); 

        ActiveSession session = ActiveSession.builder()
                .user(user)
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
                .emailOtp(otp)
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
                    .otp(otp)
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

        ForgotPassword fp = forgotPasswordRepository.findByUserAndOtpAndIsUsedFalse(user, request.getOtp())
                .orElseThrow(() -> new BusinessException("Invalid/Expired OTP.", HttpStatus.BAD_REQUEST));

        fp.setUsed(true);
        forgotPasswordRepository.save(fp);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionRepository.deleteByUser(user); 
        return "Password updated successfully.";
    }

    @Override
    @Transactional
    public String setPasswordForGoogleUser(String email, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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
}