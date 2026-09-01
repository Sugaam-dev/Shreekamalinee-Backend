package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.Constant.AuthProvider;
import com.pmrgsolution.Constant.Role;
import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.core.security.JwtUtils;
import com.pmrgsolution.features.audit.service.AuditLogService;
import com.pmrgsolution.features.auth.dto.*;
import com.pmrgsolution.features.auth.entity.ActiveSession;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.entity.UserEmailOtp;
import com.pmrgsolution.features.auth.repository.ActiveSessionRepository;
import com.pmrgsolution.features.auth.repository.ForgotPasswordRepository;
import com.pmrgsolution.features.auth.repository.UserEmailOtpRepository;
import com.pmrgsolution.features.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserEmailOtpRepository emailOtpRepository;
    @Mock private ForgotPasswordRepository forgotPasswordRepository;
    @Mock private ActiveSessionRepository sessionRepository;
    @Mock private EmailService emailService;
    @Mock private JwtUtils jwtUtils;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpGenerator otpGenerator;
    @Mock private AuditLogService auditLogService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        authService.setAuthenticationManager(authenticationManager);

        activeUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya@test.com")
                .password("$2a$10$hashedpassword")
                .role(Role.USER)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .provider(AuthProvider.LOCAL)
                .build();
    }

    // ─────────────────── LOGIN TESTS ────────────────────

    @Test
    @DisplayName("login_success: Valid credentials return AuthResponse with JWT")
    void login_success() {
        LoginRequest req = new LoginRequest("priya@test.com", "ValidPass@1");

        when(userRepository.findByEmailIgnoreCase("priya@test.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("priya@test.com", "ValidPass@1"));
        ActiveSession session = ActiveSession.builder()
                .sessionId(UUID.randomUUID())
                .refreshToken("refresh-token")
                .user(activeUser)
                .build();
        when(sessionRepository.save(any())).thenReturn(session);
        when(jwtUtils.generateToken(anyString(), anyString())).thenReturn("jwt-token");

        AuthResponse resp = authService.login(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getEmail()).isEqualTo("priya@test.com");
        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
        verify(userRepository).findByEmailIgnoreCase("priya@test.com");
    }

    @Test
    @DisplayName("login_wrongPassword: Increments failedLoginAttempts on bad password")
    void login_wrongPassword_incrementsFailedAttempts() {
        LoginRequest req = new LoginRequest("priya@test.com", "WrongPass@1");

        when(userRepository.findByEmailIgnoreCase("priya@test.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userRepository.save(any())).thenReturn(activeUser);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class);

        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("login_5thFailure: Locks the account after 5 bad password attempts")
    void login_fifthFailure_locksAccount() {
        activeUser.setFailedLoginAttempts(4); // 4 previous failures — this attempt is the 5th
        LoginRequest req = new LoginRequest("priya@test.com", "WrongPass@5");

        when(userRepository.findByEmailIgnoreCase("priya@test.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userRepository.save(any())).thenReturn(activeUser);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class);

        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(activeUser.getLockoutUntil()).isNotNull();
        assertThat(activeUser.getLockoutUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("login_lockedAccount: Throws FORBIDDEN when account is locked")
    void login_lockedAccount_throwsForbidden() {
        activeUser.setLockoutUntil(LocalDateTime.now().plusMinutes(10));
        LoginRequest req = new LoginRequest("priya@test.com", "AnyPass@1");

        when(userRepository.findByEmailIgnoreCase("priya@test.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("login_googleAccount: Refuses password login for Google-registered accounts")
    void login_googleAccount_refusesPasswordLogin() {
        activeUser.setProvider(AuthProvider.GOOGLE);
        activeUser.setPassword(null);
        LoginRequest req = new LoginRequest("priya@test.com", "SomePass@1");

        when(userRepository.findByEmailIgnoreCase("priya@test.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("login_userNotFound: Throws when email does not exist")
    void login_userNotFound_throws() {
        when(userRepository.findByEmailIgnoreCase("nobody@test.com")).thenReturn(Optional.empty());
        LoginRequest req = new LoginRequest("nobody@test.com", "Pass@1234");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class);
    }

    // ─────────────────── REGISTER & OTP TESTS ────────────────────

    @Test
    @DisplayName("register_success_new_user")
    void register_success_new_user() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("newuser@test.com");
        req.setPassword("Password@123");
        req.setFirstName("New");
        req.setLastName("User");

        when(userRepository.findByEmailIgnoreCase("newuser@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(otpGenerator.generateOtp()).thenReturn("123456");

        String result = authService.register(req);

        assertThat(result).contains("Registration successful");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("verifyOtp_success")
    void verifyOtp_success() {
        OtpVerificationRequest req = new OtpVerificationRequest();
        req.setEmail("priya@test.com");
        req.setOtp("123456");

        UserEmailOtp emailOtp = UserEmailOtp.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .emailOtp("123456")
                .isUsed(false)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(userRepository.findByEmailIgnoreCase("priya@test.com")).thenReturn(Optional.of(activeUser));
        when(emailOtpRepository.findByUserAndEmailOtpAndIsUsedFalse(activeUser, "123456")).thenReturn(Optional.of(emailOtp));
        ActiveSession session = ActiveSession.builder()
                .sessionId(UUID.randomUUID())
                .refreshToken("new-refresh-token")
                .user(activeUser)
                .build();
        when(sessionRepository.save(any())).thenReturn(session);
        when(jwtUtils.generateToken(anyString(), anyString())).thenReturn("jwt-token-verified");

        AuthResponse resp = authService.verifyOtp(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getEmail()).isEqualTo("priya@test.com");
        assertThat(activeUser.isEnabled()).isTrue();
    }
}
