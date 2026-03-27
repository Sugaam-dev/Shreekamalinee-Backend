package com.PMRGSolution.RENAISSANCE.core.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.PMRGSolution.RENAISSANCE.features.auth.repository.ActiveSessionRepository;

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

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("renaissance-jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                }
            }
        }

        // 🚨 No JWT cookie → user not logged in
        if (jwt == null) {
            log.warn("Logout attempted but user is not logged in");
            throw new AuthenticationCredentialsNotFoundException("User is not logged in");
        }

        String sessionIdStr = jwtUtils.getSessionIdFromJwtToken(jwt);
        UUID sessionId = UUID.fromString(sessionIdStr);

        boolean exists = sessionRepository.findBySessionId(sessionId).isPresent();

        // 🚨 Session already removed
        if (!exists) {
            log.warn("Logout attempted but session already logged out: {}", sessionId);
            throw new AuthenticationCredentialsNotFoundException("Session already logged out");
        }

//        log.info("Deleting session {}", sessionId);

        sessionRepository.deleteBySessionId(sessionId);
    }
}