package com.PMRGSolution.RENAISSANCE.features.exam.repository;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.TestResult;

import jakarta.transaction.Transactional;

import com.PMRGSolution.RENAISSANCE.Constant.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {
    
    List<TestResult> findByStatusAndExpiryTimeBefore(ResultStatus status, LocalDateTime now);

    Optional<TestResult> findByUserIdAndExamIdAndStatus(UUID userId, UUID examId, ResultStatus status);

    // --- UNIQUE EXAM QUOTA CHECK ---
    
    /**
     * Option A Logic: Counts how many DISTINCT exams a user has started in a specific category.
     * This is the "Gold Standard" for package limits.
     */
    @Query("SELECT COUNT(DISTINCT tr.exam.id) FROM TestResult tr " +
           "WHERE tr.user.email = :email AND tr.exam.category.id = :categoryId")
    long countUniqueExamsStartedInCategory(@Param("email") String email, @Param("categoryId") UUID categoryId);

    /**
     * Checks if a user has already started this specific exam before.
     */
    boolean existsByUserEmailAndExamId(String email, UUID examId);
    
    /**
     * GHOST PROCTOR CORE: Bulk updates all expired sessions in one SQL call.
     * Replaces the slow Java loop for 50,000+ user scalability.
     */
    @Modifying
    @Transactional
    @Query("UPDATE TestResult tr " +
           "SET tr.status = com.PMRGSolution.RENAISSANCE.Constant.ResultStatus.UNDER_EVALUATION, " +
           "    tr.submittedAt = tr.expiryTime, " +
           "    tr.fullyEvaluated = false " +
           "WHERE tr.status = com.PMRGSolution.RENAISSANCE.Constant.ResultStatus.IN_PROGRESS " +
           "AND tr.expiryTime < :deadline")
    int bulkFinalizeExpiredSessions(@Param("deadline") LocalDateTime deadline);

    // --- GENERAL FETCHING ---
    long countByUserEmailAndExamIdAndStatus(String email, UUID examId, ResultStatus status);
    List<TestResult> findByUserId(UUID userId);
    
    @Query("SELECT tr FROM TestResult tr WHERE tr.user.email = :email ORDER BY tr.startTime DESC")
    List<TestResult> findAllByUserEmail(@Param("email") String email);
}