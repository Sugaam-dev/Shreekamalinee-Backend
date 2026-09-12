package com.pmrgsolution.core.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pmrgsolution.features.auth.entity.ActiveSession;
import com.pmrgsolution.features.auth.repository.ActiveSessionRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final ActiveSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            // 1. If cookie/token is present, attempt authentication
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String email = jwtUtils.getEmailFromJwtToken(jwt);
                String sessionIdStr = jwtUtils.getSessionIdFromJwtToken(jwt);

                if (email != null && sessionIdStr != null) {
                    try {
                        UUID sessionId = UUID.fromString(sessionIdStr);
                        Optional<ActiveSession> sessionOpt = sessionRepository.findBySessionId(sessionId);

                        if (sessionOpt.isPresent()) {
                            ActiveSession session = sessionOpt.get();

                            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                sessionIdStr,
                                                userDetails.getAuthorities()
                                        );
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }

                            // Optimized Heartbeat (Max once every 1 minute)
                            LocalDateTime now = LocalDateTime.now();
                            if (session.getLastActive() == null || session.getLastActive().isBefore(now.minusMinutes(1))) {
                                session.setLastActive(now);
                                sessionRepository.save(session);
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        log.debug("Invalid session UUID format in token: {}", sessionIdStr);
                    }
                }
            }

            // 2. Continue the filter chain.
            // Spring Security authorizeHttpRequests handles public vs protected routes centrally.
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Security Filter Exception for {}: {}", request.getServletPath(), e.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Define paths that should bypass this filter entirely.
     * Includes Auth public actions and Swagger documentation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/login") || 
               path.startsWith("/api/v1/auth/register") || 
               path.startsWith("/api/v1/auth/verify-otp") ||
               path.startsWith("/api/v1/auth/google-authenticate") ||
               path.startsWith("/api/v1/auth/forgot-password") ||
               path.startsWith("/api/v1/auth/reset-password") ||
               path.startsWith("/api/v1/auth/refresh-token") ||
               path.startsWith("/api/auth/login") || 
               path.startsWith("/api/auth/register") || 
               path.startsWith("/api/auth/verify-otp") ||
               path.startsWith("/api/auth/google-authenticate") ||
               path.startsWith("/api/auth/forgot-password") ||
               path.startsWith("/api/auth/reset-password") ||
               path.startsWith("/api/auth/refresh-token") ||
               path.startsWith("/api/v1/orders/razorpay/webhook") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui");
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String json = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                message
        );
        response.getWriter().write(json);
    }

    private String parseJwt(HttpServletRequest request) {
        // 1. Check Authorization Bearer Header (Standard for Postman & Mobile apps)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Check Cookie (Standard for Web browsers)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("shreekamalinee-jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}