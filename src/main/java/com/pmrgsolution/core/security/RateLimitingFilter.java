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

    private final Map<String, TimestampedBucket> authCheckoutCache = new ConcurrentHashMap<>();
    private final Map<String, TimestampedBucket> generalCache = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ip = getClientIP(httpRequest);
        String uri = httpRequest.getRequestURI();

        // Whitelist webhooks, actuator probes, and swagger docs from rate limiting
        if (uri.startsWith("/api/v1/orders/razorpay/webhook") || 
            uri.startsWith("/actuator/") || 
            uri.startsWith("/v3/api-docs") || 
            uri.startsWith("/swagger-ui")) {
            chain.doFilter(request, response);
            return;
        }

        boolean isAuthOrCheckout = uri.startsWith("/api/auth/") || uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/orders");
        String cacheKey = ip + ":" + (isAuthOrCheckout ? "AUTH_CHECKOUT" : "GENERAL");

        TimestampedBucket entry;
        if (isAuthOrCheckout) {
            entry = authCheckoutCache.computeIfAbsent(cacheKey, k -> new TimestampedBucket(createAuthCheckoutBucket()));
        } else {
            entry = generalCache.computeIfAbsent(cacheKey, k -> new TimestampedBucket(createGeneralBucket()));
        }

        entry.touch();
        ConsumptionProbe probe = entry.bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            httpResponse.setHeader("x-rate-limit-remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("x-rate-limit-remaining", "0");
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            httpResponse.setHeader("x-rate-limit-retry-after", String.valueOf(Math.max(1, retryAfterSeconds)));
            httpResponse.getWriter().write(
                    "{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again after " 
                    + Math.max(1, retryAfterSeconds) + " seconds.\"}"
            );
        }
    }

    /**
     * Periodic cleanup scheduler to prevent unbounded memory growth.
     * Evicts IP buckets that have been inactive for more than 10 minutes.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void evictStaleBuckets() {
        long now = System.currentTimeMillis();
        authCheckoutCache.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessed) > ENTRY_TTL_MS);
        generalCache.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessed) > ENTRY_TTL_MS);
    }

    private Bucket createAuthCheckoutBucket() {
        // Strict limit: 10 requests per minute
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket createGeneralBucket() {
        // Regular limit: 60 requests per minute
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(60)
                        .refillIntervally(60, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
