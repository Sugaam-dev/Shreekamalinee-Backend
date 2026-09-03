package com.pmrgsolution.features.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Scheduler to periodically cancel PENDING orders that were abandoned without payment.
 *
 * PERF FIX (OPS-5): Added a Redis distributed lock to prevent duplicate execution
 * in multi-instance (load-balanced) deployments. Without the lock, all instances
 * would run this job simultaneously, causing the same orders to be cancelled multiple
 * times and stock to be restored multiple times.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedCartScheduler {

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "scheduler:abandoned-orders:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    @Scheduled(cron = "0 0 * * * *") // Every hour at the top of the hour
    public void cleanupAbandonedOrders() {
        // Try to acquire the distributed lock — only one instance should run this job
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("Abandoned order cleanup already running on another instance — skipping.");
            return;
        }

        try {
            log.info("Acquired distributed lock. Running abandoned order cleanup...");
            orderService.processAbandonedOrders();
        } catch (Exception e) {
            log.error("Error executing abandoned order cleanup scheduler: {}", e.getMessage());
        } finally {
            // Release the lock after completion so other instances can pick it up next run
            redisTemplate.delete(LOCK_KEY);
        }
    }
}