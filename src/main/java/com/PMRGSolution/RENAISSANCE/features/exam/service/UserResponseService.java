package com.PMRGSolution.RENAISSANCE.features.exam.service;

import com.PMRGSolution.RENAISSANCE.features.exam.dto.TestSubmissionRequest;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.SubmissionSummaryResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.TestResultResponse;
import java.util.UUID;

public interface UserResponseService {
    
    /**
     * Finalizes the exam, calculates objective scores, and marks for manual grading.
     */
    TestResultResponse submitExam(TestSubmissionRequest request, String userEmail);
    
    /**
     * Saves progress during the exam (Auto-save/Heartbeat for answers).
     */
    void saveProgress(TestSubmissionRequest.AnswerDto answerDto, UUID resultId, UUID userId);

    /**
     * Admin tool to grade subjective/sketch questions.
     */
    void saveManualGrade(UUID responseId, Double assignedMarks);
    public SubmissionSummaryResponse getSummary(UUID resultId);
}