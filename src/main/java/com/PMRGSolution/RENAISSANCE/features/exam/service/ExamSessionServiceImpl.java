package com.PMRGSolution.RENAISSANCE.features.exam.service;

import com.PMRGSolution.RENAISSANCE.Constant.*;
import com.PMRGSolution.RENAISSANCE.Exception.*;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.UserRepository;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.UserSubscription;
import com.PMRGSolution.RENAISSANCE.features.payment.repository.UserSubscriptionRepository;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.ExamPaperResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.*;
import com.PMRGSolution.RENAISSANCE.features.exam.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private final TestResultRepository testResultRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final ExamService examService;

    @Override
    @Transactional
    public ExamPaperResponse startOrResumeExam(UUID examId, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        // 1. RESUME CHECK: If session exists and is valid, return it
        Optional<TestResult> existing = testResultRepository
                .findByUserIdAndExamIdAndStatus(user.getId(), examId, ResultStatus.IN_PROGRESS);
        
        if (existing.isPresent()) {
            TestResult session = existing.get();
            if (LocalDateTime.now().isAfter(session.getExpiryTime())) {
                finalizeSession(session);
                throw new BusinessException("Session time expired and has been auto-submitted.");
            }
            return examService.getExamPaper(examId, userEmail);
        }

        // 2. STRICT ACCESS VALIDATION: Every exam is now a paid resource
        validateStrictAccess(userEmail, exam);

        // 3. CREATE NEW SESSION
        LocalDateTime now = LocalDateTime.now();
        TestResult session = TestResult.builder()
                .user(user)
                .exam(exam)
                .startTime(now)
                .expiryTime(now.plusMinutes(exam.getDurationMinutes()))
                .lastHeartbeat(now)
                .status(ResultStatus.IN_PROGRESS)
                .totalMarksPossible(exam.getTotalMarks())
                .fullyEvaluated(false)
                .build();

        testResultRepository.save(session);
        return examService.getExamPaper(examId, userEmail);
    }

    private void validateStrictAccess(String email, Exam exam) {
        // A. FETCH SUBSCRIPTION: No bypass. Every exam needs a sub.
        UserSubscription sub = subscriptionRepository.findActiveSubscription(
                email, exam.getCategory().getId(), LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Access Denied: This exam requires an active subscription."));

        // B. TIER RANK CHECK: User must meet Admin's required tier
        if (sub.getTier().getRank() < exam.getRequiredTier().getRank()) {
            throw new BusinessException("Upgrade required: This content requires " + exam.getRequiredTier().name() + " access.");
        }

        // C. UNIVERSAL QUOTA CHECK: Every exam consumes 1 unique slot from the Package limit
        Integer limit = sub.getProductPackage().getMockTestLimit();
        
        if (limit == null || limit <= 0) {
            throw new BusinessException("Your current package allows zero exams. Please upgrade.");
        }

        long uniqueStartedCount = testResultRepository.countUniqueExamsStartedInCategory(
                email, exam.getCategory().getId());

        boolean alreadyStartedThisExam = testResultRepository.existsByUserEmailAndExamId(email, exam.getId());
        
        if (!alreadyStartedThisExam && uniqueStartedCount >= limit) {
            throw new BusinessException("Limit reached! Your package only allows " + limit + " unique exams.", HttpStatus.PAYMENT_REQUIRED);
        }
    }

    @Override
    @Transactional
    public void updateHeartbeat(UUID sessionId, String userEmail) {
        TestResult session = testResultRepository.findById(sessionId).orElseThrow();
        if (LocalDateTime.now().isAfter(session.getExpiryTime())) {
            finalizeSession(session);
            throw new BusinessException("Exam time expired.");
        }
        session.setLastHeartbeat(LocalDateTime.now());
        testResultRepository.save(session);
    }

    @Override
    @Transactional
    public void processExpiredSessions() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(2);
        List<TestResult> expired = testResultRepository.findByStatusAndExpiryTimeBefore(ResultStatus.IN_PROGRESS, deadline);
        expired.forEach(this::finalizeSession);
    }

    private void finalizeSession(TestResult session) {
        session.setStatus(ResultStatus.UNDER_EVALUATION);
        session.setSubmittedAt(session.getExpiryTime());
        session.setFullyEvaluated(false);
        testResultRepository.save(session);
    }
}