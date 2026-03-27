package com.PMRGSolution.RENAISSANCE.core.config;

import com.PMRGSolution.RENAISSANCE.features.exam.service.ExamSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamScheduler {

    private final ExamSessionService examSessionService;

    /**
     * Runs every 60 seconds to trigger the session cleanup.
     * Uses fixedDelay to ensure one task finishes before the next starts.
     */
    @Scheduled(fixedDelay = 60000)
    public void autoSubmitExpiredExams() {
        log.debug("ExamScheduler: Triggering auto-submission check...");
        try {
            examSessionService.processExpiredSessions();
        } catch (Exception e) {
            log.error("ExamScheduler Error: Failed to process expired sessions: {}", e.getMessage());
        }
    }
}