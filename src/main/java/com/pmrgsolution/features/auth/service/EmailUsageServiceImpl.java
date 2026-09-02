package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.auth.dto.EmailStatsResponse;
import com.pmrgsolution.features.auth.dto.PublicEmailStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class EmailUsageServiceImpl implements EmailUsageService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.mail.daily-limit:100}")
    private long dailyLimit;

    @Value("${app.mail.monthly-limit:3000}")
    private long monthlyLimit;

    // In-memory fallback if Redis is not active
    private final ConcurrentHashMap<String, AtomicLong> localDailyStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> localMonthlyStats = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public EmailUsageServiceImpl(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordEmailSent() {
        String today = LocalDate.now(ZoneOffset.UTC).format(DAY_FORMATTER);
        String thisMonth = LocalDate.now(ZoneOffset.UTC).format(MONTH_FORMATTER);

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                String dailyKey = "email_stats:daily:" + today;
                String monthlyKey = "email_stats:monthly:" + thisMonth;

                Long dailyVal = redisTemplate.opsForValue().increment(dailyKey);
                redisTemplate.expire(dailyKey, 3, TimeUnit.DAYS);

                Long monthlyVal = redisTemplate.opsForValue().increment(monthlyKey);
                redisTemplate.expire(monthlyKey, 45, TimeUnit.DAYS);

                log.info("Email recorded in Redis counter: Today={}, Month={}", dailyVal, monthlyVal);
                return;
            }
        } catch (Exception e) {
            log.debug("Redis unavailable for email stats, using in-memory tracker: {}", e.getMessage());
        }

        // Fallback to in-memory counter
        localDailyStats.computeIfAbsent(today, k -> new AtomicLong(0)).incrementAndGet();
        localMonthlyStats.computeIfAbsent(thisMonth, k -> new AtomicLong(0)).incrementAndGet();
    }

    @Override
    public void recordEmailFailed() {
        // Can be extended if failure metrics are needed separately
    }

    @Override
    public EmailStatsResponse getEmailStats() {
        String today = LocalDate.now(ZoneOffset.UTC).format(DAY_FORMATTER);
        String thisMonth = LocalDate.now(ZoneOffset.UTC).format(MONTH_FORMATTER);

        long sentToday = getCounterValue("email_stats:daily:" + today, localDailyStats, today);
        long sentThisMonth = getCounterValue("email_stats:monthly:" + thisMonth, localMonthlyStats, thisMonth);

        long remainingToday = Math.max(0, dailyLimit - sentToday);
        long remainingThisMonth = Math.max(0, monthlyLimit - sentThisMonth);
        boolean dailyQuotaExceeded = sentToday >= dailyLimit;
        boolean monthlyQuotaExceeded = sentThisMonth >= monthlyLimit;

        return EmailStatsResponse.builder()
                .sentToday(sentToday)
                .dailyLimit(dailyLimit)
                .remainingToday(remainingToday)
                .dailyQuotaExceeded(dailyQuotaExceeded)
                .sentThisMonth(sentThisMonth)
                .monthlyLimit(monthlyLimit)
                .remainingThisMonth(remainingThisMonth)
                .monthlyQuotaExceeded(monthlyQuotaExceeded)
                .quotaResetTime("12:00 AM UTC")
                .build();
    }

    @Override
    public PublicEmailStatusResponse getPublicEmailStatus() {
        EmailStatsResponse stats = getEmailStats();
        boolean exceeded = stats.isDailyQuotaExceeded() || stats.isMonthlyQuotaExceeded();

        String announcement = exceeded
                ? "Our automated email verification has reached today's limit. Please use 'Sign in with Google' for instant access."
                : "Email delivery operational.";

        return PublicEmailStatusResponse.builder()
                .dailyQuotaExceeded(exceeded)
                .serviceActive(true)
                .announcement(announcement)
                .quotaResetTime("12:00 AM UTC")
                .build();
    }

    private long getCounterValue(String redisKey, ConcurrentHashMap<String, AtomicLong> localMap, String localKey) {
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                String val = redisTemplate.opsForValue().get(redisKey);
                if (val != null) {
                    return Long.parseLong(val.trim());
                }
            }
        } catch (Exception e) {
            log.debug("Could not read Redis email stat: {}", e.getMessage());
        }

        AtomicLong counter = localMap.get(localKey);
        return counter != null ? counter.get() : 0L;
    }
}
