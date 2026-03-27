package com.PMRGSolution.RENAISSANCE.core.security;

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

import com.PMRGSolution.RENAISSANCE.features.auth.entity.ActiveSession;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.ActiveSessionRepository;

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

            // 1. If no cookie is present, continue the chain. 
            // Spring Security will catch unauthorized access to protected routes later.
            if (jwt == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Validate Token integrity
            if (!jwtUtils.validateJwtToken(jwt)) {
                log.warn("Invalid or expired JWT attempted for path: {}", request.getServletPath());
                sendErrorResponse(response, "Session expired. Please login again.");
                return;
            }

            String email = jwtUtils.getEmailFromJwtToken(jwt);
            String sessionIdStr = jwtUtils.getSessionIdFromJwtToken(jwt);

            if (sessionIdStr == null) {
                sendErrorResponse(response, "Corrupted session. Please re-authenticate.");
                return;
            }

            // 3. Single Device Enforcement: Verify session exists in DB
            UUID sessionId = UUID.fromString(sessionIdStr);
            Optional<ActiveSession> sessionOpt = sessionRepository.findBySessionId(sessionId);

            if (sessionOpt.isEmpty()) {
                log.warn("Session {} for {} not found in DB. Possible forced logout.", sessionId, email);
                sendErrorResponse(response, "Session terminated by another login.");
                return;
            }

         // Inside your doFilterInternal method...

            ActiveSession session = sessionOpt.get();

            // 4. Authenticate the User in the Security Context
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

            // --- 5. OPTIMIZED HEARTBEAT (Fixed Lag) ---
            LocalDateTime now = LocalDateTime.now();
            // Only update DB if the last heartbeat was more than 1 minute ago
            if (session.getLastActive() == null || session.getLastActive().isBefore(now.minusMinutes(1))) {
                session.setLastActive(now);
                sessionRepository.save(session); 
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Security Filter Exception: {}", e.getMessage());
            sendErrorResponse(response, "Authentication system error.");
        }
    }

    /**
     * Define paths that should bypass this filter entirely.
     * Includes Auth public actions and Swagger documentation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/login") || 
               path.startsWith("/api/auth/register") || 
               path.startsWith("/api/auth/verify-otp") ||
               path.startsWith("/api/auth/forgot-password") ||
               path.startsWith("/api/auth/reset-password") ||
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
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("renaissance-jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}