package com.pmrgsolution.features.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.core.security.JwtAuthenticationFilter;
import com.pmrgsolution.core.security.JwtUtils;
import com.pmrgsolution.core.security.LogoutService;
import com.pmrgsolution.features.auth.dto.AuthResponse;
import com.pmrgsolution.features.auth.dto.LoginRequest;
import com.pmrgsolution.features.auth.dto.RegistrationRequest;
import com.pmrgsolution.features.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Integration / Slice Tests")
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private LogoutService logoutService;

    @Test
    @DisplayName("POST /api/v1/auth/register -> 200 OK")
    void register_success() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("test@example.com");
        req.setPassword("Password@123");
        req.setFirstName("Test");
        req.setLastName("User");
        req.setPhoneNumber("+919876543210");

        when(authService.register(any(RegistrationRequest.class)))
                .thenReturn("Registration successful. Verify your email via OTP.");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful. Verify your email via OTP."));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login -> 200 OK with AuthResponse")
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "Password@123");
        AuthResponse authResp = AuthResponse.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("USER")
                .token("jwt-mock-token")
                .refreshToken("refresh-mock-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResp);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-mock-token"));
    }
}
