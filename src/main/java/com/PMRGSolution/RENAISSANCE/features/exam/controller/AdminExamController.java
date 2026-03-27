package com.PMRGSolution.RENAISSANCE.features.exam.controller;

import com.PMRGSolution.RENAISSANCE.features.exam.dto.BulkExamRequest;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.Exam;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.Question;
import com.PMRGSolution.RENAISSANCE.features.exam.service.ExamService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
public class AdminExamController {

    private final ExamService examService;

    @PostMapping("/bulk-upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UUID> createFullExam(@RequestBody BulkExamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.saveFullExam(request));
    }

    // --- NEW: Lifecycle Control ---
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> togglePublish(@PathVariable UUID id, @RequestParam boolean status) {
        examService.togglePublishStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // --- NEW: Single Question Management ---
    @PostMapping("/sections/{sectionId}/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Question> addQuestion(@PathVariable UUID sectionId, @RequestBody BulkExamRequest.QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.addQuestionToSection(sectionId, request));
    }

    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Question> updateQuestion(@PathVariable UUID questionId, @RequestBody BulkExamRequest.QuestionRequest request) {
        return ResponseEntity.ok(examService.updateQuestion(questionId, request));
    }

   

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Exam>> getAllExamsForAdmin() {
        return ResponseEntity.ok(examService.getAllActiveExams()); 
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Permanently delete an entire exam and all its questions")
    public ResponseEntity<Void> deleteExam(@PathVariable UUID id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Delete a specific question and update exam total marks")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID questionId) {
        examService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}