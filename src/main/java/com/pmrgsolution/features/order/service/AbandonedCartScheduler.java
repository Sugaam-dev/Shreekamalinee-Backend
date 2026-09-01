package com.pmrgsolution.features.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedCartScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupAbandonedOrders() {
        try {
            orderService.processAbandonedOrders();
        } catch (Exception e) {
            log.error("Error executing abandoned order cleanup scheduler: {}", e.getMessage());
        }
    }
}