package com.pmrgsolution.features.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Scheduled cleanup job to remove expired OTP records from the database.
 *
 * Without this, the forgot_password and user_email_otp tables grow unboundedly
 * over time, slowing down queries on those tables.
 *
 * Runs once per day at 02:00 AM. Uses a distributed Redis lock to ensure
 * only one instance executes the cleanup in multi-instance deployments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "scheduler:otp-cleanup:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    @Scheduled(cron = "0 0 2 * * *") // Every day at 02:00 AM
    public void cleanupExpiredOtps() {
        // Acquire distributed lock to prevent duplicate runs in multi-instance deployments
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("OTP cleanup already running on another instance — skipping.");
            return;
        }

        try {
            // Delete expired (or used) registration OTPs
            int otpDeleted = jdbcTemplate.update(
                    "DELETE FROM user_email_otp WHERE expires_at < NOW() OR is_used = true");

            // Delete expired (or used) password reset OTPs
            int fpDeleted = jdbcTemplate.update(
                    "DELETE FROM forgot_password WHERE expires_at < NOW() OR is_used = true");

            log.info("OTP cleanup complete — deleted {} registration OTPs, {} password reset OTPs.",
                    otpDeleted, fpDeleted);
        } catch (Exception e) {
            log.error("Error during OTP cleanup: {}", e.getMessage());
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
