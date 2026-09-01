package com.pmrgsolution.core.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pmrgsolution.features.auth.repository.ActiveSessionRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private final ActiveSessionRepository sessionRepository;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {

        String jwt = null;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("shreekamalinee-jwt".equals(cookie.getName()) || "renaissance-jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                }
            }
        }

        // No JWT cookie → user already logged out
        if (jwt == null) {
            log.info("Logout attempted but no JWT cookie was found");
            return;
        }

        try {
            String sessionIdStr = jwtUtils.getSessionIdFromJwtToken(jwt);
            UUID sessionId = UUID.fromString(sessionIdStr);

            sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                sessionRepository.delete(session);
                log.info("Successfully deleted active session: {}", sessionId);
            });
        } catch (Exception e) {
            log.warn("Logout error or invalid/expired JWT: {}", e.getMessage());
        }
    }
}