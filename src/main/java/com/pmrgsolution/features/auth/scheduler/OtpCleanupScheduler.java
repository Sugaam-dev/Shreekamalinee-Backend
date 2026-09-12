package com.pmrgsolution.features.auth.scheduler;

import com.pmrgsolution.features.auth.repository.ForgotPasswordRepository;
import com.pmrgsolution.features.auth.repository.UserEmailOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpCleanupScheduler {

    private final UserEmailOtpRepository userEmailOtpRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "scheduler:otp-cleanup:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        Boolean acquired = Boolean.FALSE;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
                if (!Boolean.TRUE.equals(acquired)) {
                    log.debug("OTP cleanup already running on another instance — skipping.");
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Redis lock unavailable for OTP cleanup, proceeding with single-run execution: {}", e.getMessage());
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            int otpDeleted = userEmailOtpRepository.deleteExpiredOrUsed(now);
            int fpDeleted = forgotPasswordRepository.deleteExpiredOrUsed(now);

            log.info("OTP cleanup complete — deleted {} registration OTPs, {} password reset OTPs.",
                    otpDeleted, fpDeleted);
        } catch (Exception e) {
            log.error("Error during OTP cleanup: {}", e.getMessage());
        } finally {
            try {
                if (Boolean.TRUE.equals(acquired) && redisTemplate != null) {
                    redisTemplate.delete(LOCK_KEY);
                }
            } catch (Exception ignored) {}
        }
    }
}