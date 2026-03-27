package com.PMRGSolution.RENAISSANCE.features.exam.service;

import com.PMRGSolution.RENAISSANCE.features.exam.dto.ExamPaperResponse;
import java.util.UUID;

public interface ExamSessionService {
    
    // UPDATED: Now returns the session ID + Paper for a seamless start
    ExamPaperResponse startOrResumeExam(UUID examId, String userEmail);

    void updateHeartbeat(UUID sessionId, String userEmail);

    void processExpiredSessions();
}