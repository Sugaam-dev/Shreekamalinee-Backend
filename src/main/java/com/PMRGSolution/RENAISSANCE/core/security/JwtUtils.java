package com.PMRGSolution.RENAISSANCE.core.security;

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

    @Value("${renaissance.jwt.secret}")
    private String jwtSecret;

    @Value("${renaissance.jwt.expirationMs}")
    private int jwtExpirationMs;

    private Key key;
    private JwtParser jwtParser;

    /**
     * Initialize the signing key and the parser once to improve performance.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parserBuilder()
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
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        }
        return false;
    }
}