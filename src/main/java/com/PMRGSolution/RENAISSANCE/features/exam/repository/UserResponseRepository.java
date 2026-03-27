package com.PMRGSolution.RENAISSANCE.features.exam.repository;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.UserResponse;
import com.PMRGSolution.RENAISSANCE.Constant.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserResponseRepository extends JpaRepository<UserResponse, UUID> {

    /**
     * Used for the "Upsert" logic in saveProgress.
     * Checks if the user has already saved an answer for this specific question in this session.
     */
    Optional<UserResponse> findByTestResultIdAndQuestionId(UUID testResultId, UUID questionId);

    /**
     * Admin Grading Queue: Find all responses of a specific type (e.g., SUBJECTIVE) 
     * that haven't been assigned marks yet.
     */
    @Query("SELECT ur FROM UserResponse ur JOIN ur.testResult tr " +
           "WHERE ur.question.type = :type " +
           "AND ur.evaluated = false " +
           "AND tr.status = 'UNDER_EVALUATION'")
    List<UserResponse> findUngradedSubjectiveResponses(@Param("type") QuestionType type);

    /**
     * Fetch all responses for a specific exam attempt (used in submission and result display).
     */
    List<UserResponse> findByTestResultId(UUID testResultId);

    /**
     * Dashboard Metric: Count how many responses are pending manual grading across the system.
     */
    long countByEvaluatedFalseAndQuestionType(QuestionType type);
}