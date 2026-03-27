package com.PMRGSolution.RENAISSANCE.features.exam.controller;

import com.PMRGSolution.RENAISSANCE.core.security.CustomUserDetails;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.ExamPaperResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.ExamListResponse; // New DTO
import com.PMRGSolution.RENAISSANCE.features.exam.dto.SubmissionSummaryResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.service.ExamService;
import com.PMRGSolution.RENAISSANCE.features.exam.service.ExamSessionService;
import com.PMRGSolution.RENAISSANCE.features.exam.service.UserResponseService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final ExamSessionService examSessionService;
    private final UserResponseService userResponseService;

    /**
     * DISCOVERY: Returns metadata only. 
     * Questions are HIDDEN here to prevent cheating before payment.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ExamListResponse>> getExamsByCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal CustomUserDetails user) {
        // This method will now return a DTO with isLocked based on user rank
        return ResponseEntity.ok(examService.getExamsForUserByCategory(categoryId, user.getUsername()));
    }

    /**
     * SECURE START: This is the ONLY endpoint that returns questions.
     * It runs the Tier Rank and Mock Limit validation.
     */
    @GetMapping("/{id}/start")
    public ResponseEntity<ExamPaperResponse> startExam(
            @PathVariable UUID id, 
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(examSessionService.startOrResumeExam(id, user.getUsername()));
    }

    @PatchMapping("/sessions/{sessionId}/heartbeat")
    public ResponseEntity<Void> sendHeartbeat(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        examSessionService.updateHeartbeat(sessionId, user.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ResponseEntity<SubmissionSummaryResponse> getExamSummary(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userResponseService.getSummary(sessionId));
    }
}