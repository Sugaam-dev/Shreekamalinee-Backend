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
     * GHOST PROCTOR: Runs every 60 seconds (60000 ms).
     * Automatically finalizes all "IN_PROGRESS" exams that have passed their expiryTime.
     */
    @Scheduled(fixedRate = 60000)
    public void autoFinalizeExpiredExams() {
        try {
            // This calls the Step 5 Bulk Update logic in your Service
            examSessionService.processExpiredSessions();
        } catch (Exception e) {
            log.error("Ghost Proctor encountered an error during auto-finalization: ", e);
        }
    }
}