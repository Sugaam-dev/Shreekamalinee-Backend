package com.PMRGSolution.RENAISSANCE.features.exam.service;

import com.PMRGSolution.RENAISSANCE.features.exam.dto.BulkExamRequest;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.ExamListResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.ExamPaperResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.Exam;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.Question;
import java.util.List;
import java.util.UUID;

public interface ExamService {
    // --- Admin Management ---
    UUID saveFullExam(BulkExamRequest request);
    void deleteExam(UUID examId);
    void togglePublishStatus(UUID examId, boolean isPublished);
    void updateExamTotalMarks(UUID examId);

    // --- Question Management ---
    Question addQuestionToSection(UUID sectionId, BulkExamRequest.QuestionRequest dto);
    Question updateQuestion(UUID questionId, BulkExamRequest.QuestionRequest dto);
    void deleteQuestion(UUID questionId);

    // --- Secured Student Fetching ---
    Exam getExamById(UUID id);
    List<Exam> getAllPublishedExams(); 
    List<Exam> getExamsByCategory(UUID categoryId);
    List<Exam> getAllActiveExams();
    
    // Returns Exam list with dynamic 'isLocked' status
    List<ExamListResponse> getExamsForUserByCategory(UUID categoryId, String userEmail);
    
    // Returns the actual paper (questions/options)
    ExamPaperResponse getExamPaper(UUID examId, String userEmail); 
    
    // Logic to verify if a user is allowed to start based on Tier and Quota
    void validateExamAccess(UUID examId, String userEmail);
}