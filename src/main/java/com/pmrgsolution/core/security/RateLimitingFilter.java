package com.pmrgsolution.core.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise 3-Tier Proxy-Aware Rate Limiting Filter.
 * 
 * Protects critical endpoints from credential stuffing & DDoS while guaranteeing
 * zero false lockouts (HTTP 429) for legitimate customers and administrators.
 */
@Slf4j
@Component
public class RateLimitingFilter implements Filter {

    private static final long ENTRY_TTL_MS = Duration.ofMinutes(10).toMillis();

    private static class TimestampedBucket {
        final Bucket bucket;
        volatile long lastAccessed;

        TimestampedBucket(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessed = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }

    // 3 Distinct Cache Tiers for Memory Safety & Granular Throttling
    private final Map<String, TimestampedBucket> authMutationCache = new ConcurrentHashMap<>();
    private final Map<String, TimestampedBucket> sessionCheckoutCache = new ConcurrentHashMap<>();
    private final Map<String, TimestampedBucket> generalCatalogCache = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Never throttle CORS preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String uri = httpRequest.getRequestURI();

        // Whitelist webhooks, actuator probes, and swagger docs from rate limiting
        if (uri.startsWith("/api/v1/orders/razorpay/webhook") || 
            uri.startsWith("/actuator/") || 
            uri.startsWith("/v3/api-docs") || 
            uri.startsWith("/swagger-ui")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(httpRequest);
        String tier = determineTier(uri);
        String cacheKey = ip + ":" + tier;

        TimestampedBucket entry;
        switch (tier) {
            case "AUTH_MUTATION" -> entry = authMutationCache.computeIfAbsent(cacheKey, k -> new TimestampedBucket(createAuthMutationBucket()));
            case "SESSION_CHECKOUT" -> entry = sessionCheckoutCache.computeIfAbsent(cacheKey, k -> new TimestampedBucket(createSessionCheckoutBucket()));
            default -> entry = generalCatalogCache.computeIfAbsent(cacheKey, k -> new TimestampedBucket(createGeneralCatalogBucket()));
        }

        entry.touch();
        ConsumptionProbe probe = entry.bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            httpResponse.setHeader("x-rate-limit-remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on URI: {} [Tier: {}]", ip, uri, tier);
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("x-rate-limit-remaining", "0");
            long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            httpResponse.setHeader("x-rate-limit-retry-after", String.valueOf(retryAfterSeconds));
            httpResponse.getWriter().write(
                    "{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again after " 
                    + retryAfterSeconds + " seconds.\"}"
            );
        }
    }

    /**
     * Maps URI to appropriate security tier.
     */
    private String determineTier(String uri) {
        // Tier 1: Brute-Force sensitive credential & OTP endpoints
        if (uri.endsWith("/auth/login") ||
            uri.endsWith("/auth/register") ||
            uri.endsWith("/auth/verify-otp") ||
            uri.endsWith("/auth/resend-otp") ||
            uri.endsWith("/auth/forgot-password") ||
            uri.endsWith("/auth/reset-password") ||
            uri.endsWith("/auth/google-authenticate")) {
            return "AUTH_MUTATION";
        }

        // Tier 2: Authenticated user queries, admin portal management, checkout & carts
        if (uri.contains("/auth/me") ||
            uri.contains("/auth/refresh-token") ||
            uri.contains("/auth/logout") ||
            uri.contains("/auth/email-service-status") ||
            uri.startsWith("/api/v1/orders") ||
            uri.startsWith("/api/orders") ||
            uri.startsWith("/api/v1/admin") ||
            uri.startsWith("/api/v1/cart") ||
            uri.startsWith("/api/v1/wishlist") ||
            uri.startsWith("/api/v1/account") ||
            uri.startsWith("/api/v1/coupons")) {
            return "SESSION_CHECKOUT";
        }

        // Tier 3: Public catalog, product search, categories, settings, banners
        return "GENERAL_CATALOG";
    }

    /**
     * Periodic cleanup scheduler to prevent unbounded memory growth.
     * Evicts IP buckets that have been inactive for more than 10 minutes.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void evictStaleBuckets() {
        long now = System.currentTimeMillis();
        authMutationCache.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessed) > ENTRY_TTL_MS);
        sessionCheckoutCache.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessed) > ENTRY_TTL_MS);
        generalCatalogCache.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessed) > ENTRY_TTL_MS);
    }

    /**
     * Tier 1: Brute Force Limit - 15 requests/min with greedy refill (smooth token regeneration)
     */
    private Bucket createAuthMutationBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(15)
                        .refillGreedy(15, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Tier 2: Authenticated Sessions, Checkout & Admin Operations - 180 requests/min
     */
    private Bucket createSessionCheckoutBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(180)
                        .refillGreedy(180, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Tier 3: General Public Browsing & Catalog Reads - 360 requests/min
     */
    private Bucket createGeneralCatalogBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(360)
                        .refillGreedy(360, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Extracts client real IP behind Cloudflare, Nginx, or Docker reverse proxies.
     */
    private String getClientIP(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }

        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
