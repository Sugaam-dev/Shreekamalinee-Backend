package com.pmrgsolution.core.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Enterprise Utility class for JSON Web Token operations.
 * Modernized for JJWT 0.12.6 API with java.time.Instant.
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${shreekamalinee.jwt.secret}")
    private String jwtSecret;

    @Value("${shreekamalinee.jwt.expirationMs:900000}")
    private int jwtExpirationMs;

    @Value("${shreekamalinee.jwt.refreshExpirationMs:7776000000}")
    private long jwtRefreshExpirationMs;

    private SecretKey key;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT Secret is missing! Please configure JWT_SECRET in your .env file or environment variables."
            );
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret.trim());
            if (keyBytes == null || keyBytes.length < 64) {
                keyBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
        }

        // Ensure minimum 512 bits (64 bytes) for HS512 cryptographic strength
        if (keyBytes.length < 64) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                keyBytes = digest.digest(keyBytes);
            } catch (Exception ignored) {}
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(key)
                .build();
    }

    public String generateToken(String email, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("sessionId", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtExpirationMs)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("sessionId", sessionId)
                .claim("type", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtRefreshExpirationMs)))
                .signWith(key)
                .compact();
    }

    public long getRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }

    public String getEmailFromJwtToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getSessionIdFromJwtToken(String token) {
        return getClaims(token).get("sessionId", String.class);
    }

    private Claims getClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            jwtParser.parseSignedClaims(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        }
        return false;
    }
}