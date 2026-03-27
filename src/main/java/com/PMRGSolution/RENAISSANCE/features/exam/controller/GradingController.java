package com.PMRGSolution.RENAISSANCE.features.exam.controller;

import com.PMRGSolution.RENAISSANCE.features.exam.service.UserResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/grading")
@RequiredArgsConstructor
public class GradingController {

    private final UserResponseService userResponseService;

    /**
     * SAVE MANUAL MARKS:
     * Used by Admin to grade Subjective/Sketching questions.
     */
    @PatchMapping("/responses/{responseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> gradeResponse(
            @PathVariable UUID responseId, 
            @RequestParam Double marks) {
        
        userResponseService.saveManualGrade(responseId, marks);
        return ResponseEntity.ok("Marks updated and total score recalculated.");
    }
}