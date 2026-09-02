package com.pmrgsolution.core.security;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for JSON Web Token operations.
 * Handles token generation, validation, and claim extraction.
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

    private Key key;
    private JwtParser jwtParser;

    /**
     * Initialize the signing key and the parser once to improve performance.
     */
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
                keyBytes = jwtSecret.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = jwtSecret.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        // Ensure minimum 512 bits (64 bytes) for HS512 cryptographic strength
        if (keyBytes.length < 64) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");
                keyBytes = digest.digest(keyBytes);
            } catch (Exception ignored) {}
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .setSigningKey(key)
                .build();
    }

    /**
     * Generates a token with the user's email as subject and a unique sessionId claim.
     */
    public String generateToken(String email, String sessionId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("sessionId", sessionId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generates a long-lived refresh token for token rotation.
     */
    public String generateRefreshToken(String email, String sessionId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("sessionId", sessionId)
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
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

    /**
     * Internal helper to extract all claims from the token.
     */
    private Claims getClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    /**
     * Validates the integrity and expiration of the JWT.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            jwtParser.parseClaimsJws(authToken);
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