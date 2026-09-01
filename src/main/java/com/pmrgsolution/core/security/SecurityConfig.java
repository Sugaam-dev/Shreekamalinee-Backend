package com.pmrgsolution.core.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity 
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LogoutService logoutService;
    private final UserDetailsService userDetailsService;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://renaissance-zeta.vercel.app}")
    private List<String> allowedOrigins;

    // --- 1. PASSWORD ENCODER BEAN ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- 2. AUTHENTICATION PROVIDER (The Bridge) ---
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // --- 3. AUTHENTICATION MANAGER ---
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none';"))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            )
            .authenticationProvider(authenticationProvider()) // Register the provider
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication required\"}");
                })
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/verify-otp",
                        "/api/v1/auth/google-authenticate", "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password", "/api/v1/auth/refresh-token",
                        "/api/auth/login", "/api/auth/register", "/api/auth/verify-otp",
                        "/api/auth/google-authenticate", "/api/auth/forgot-password",
                        "/api/auth/reset-password", "/api/auth/refresh-token",
                        "/actuator/health", "/actuator/info",
                        "/v3/api-docs/**", "/swagger-ui/**",
                        "/api/v1/orders/razorpay/webhook",
                        "/api/v1/orders/bank-details",      // Public: needed for checkout UPI display
                        "/api/v1/settings/public",
                        "/api/v1/contact",
                        // SECURITY FIX: Only coupon VALIDATE is public (for checkout preview without login)
                        // GET /api/v1/coupons (list all) is no longer public — requires authentication
                        "/api/v1/coupons/validate",
                        "/api/v1/orders/coupons/validate"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/catalog/**", "/api/v1/settings/**").permitAll()

                .requestMatchers("/actuator/**", "/api/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")

                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                    .logoutUrl("/api/v1/auth/logout")
                    .addLogoutHandler(logoutService)
                    .deleteCookies("shreekamalinee-jwt", "shreekamalinee-refresh", "renaissance-jwt")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\": \"Logout successful\"}");
                    })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type","Authorization","x-auth-token","Accept","Idempotency-Key"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Set-Cookie", "x-rate-limit-remaining", "x-rate-limit-retry-after"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}