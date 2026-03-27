package com.PMRGSolution.RENAISSANCE.features.exam.service;

import com.PMRGSolution.RENAISSANCE.Constant.QuestionType;
import com.PMRGSolution.RENAISSANCE.Constant.ResultStatus;
import com.PMRGSolution.RENAISSANCE.Exception.BusinessException;
import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.UserRepository;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.TestSubmissionRequest;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.SubmissionSummaryResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.TestResultResponse;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.*;
import com.PMRGSolution.RENAISSANCE.features.exam.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserResponseServiceImpl implements UserResponseService {

    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final UserResponseRepository userResponseRepository;
    private final QuestionRepository questionRepository;

    /**
     * SECURE AUTO-SAVE (Heartbeat Integrated)
     * Every save action refreshes the 'lastHeartbeat' to protect the session from auto-submission.
     */
    @Override
    @Transactional
    public void saveProgress(TestSubmissionRequest.AnswerDto dto, UUID resultId, UUID userId) {
        TestResult result = testResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam session not found"));

        // 1. HARD SECURITY: Verify ownership and lifecycle
        if (!result.getUser().getId().equals(userId)) {
            log.warn("Unauthorized attempt to save progress on result {} by user {}", resultId, userId);
            throw new BusinessException("Unauthorized access to exam session.");
        }

        if (result.getStatus() != ResultStatus.IN_PROGRESS) {
            throw new BusinessException("Submission closed. This session is no longer active.");
        }

        // 2. DEADLINE CHECK: Server-side enforcement
        if (LocalDateTime.now().isAfter(result.getExpiryTime())) {
            throw new BusinessException("Time has expired. Please submit your exam.");
        }

        Question question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        // 3. UPSERT LOGIC
        UserResponse response = userResponseRepository
                .findByTestResultIdAndQuestionId(resultId, dto.getQuestionId())
                .orElse(new UserResponse());

        response.setTestResult(result);
        response.setUser(result.getUser());
        response.setQuestion(question);
        
        response.setSelectedOptionIds(dto.getSelectedOptionIds());
        response.setNumericalAnswer(dto.getNumericalAnswer());
        response.setTextAnswer(dto.getTextAnswer());
        response.setResponseStatus(dto.getResponseStatus());

        if (dto.getSketchData() != null) {
            // Secure Base64 handling
            String data = dto.getSketchData();
            String base64 = data.contains(",") ? data.split(",")[1] : data;
            response.setSketchData(Base64.getDecoder().decode(base64));
        }

        userResponseRepository.save(response);

        // 4. HEARTBEAT SYNC: Critical for background 'Ghost Proctor' logic
        result.setLastHeartbeat(LocalDateTime.now());
        testResultRepository.save(result);
    }

    @Override
    @Transactional
    public TestResultResponse submitExam(TestSubmissionRequest request, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TestResult result = testResultRepository.findByUserIdAndExamIdAndStatus(
                user.getId(), request.getExamId(), ResultStatus.IN_PROGRESS)
                .orElseThrow(() -> new BusinessException("No active session found."));

        // Professional Standard: 2-minute "Lag Grace Period" for final submission
        if (LocalDateTime.now().isAfter(result.getExpiryTime().plusMinutes(2))) {
            log.error("Rejecting late submission for user {} (Exam: {})", userEmail, request.getExamId());
            throw new BusinessException("Submission rejected: Time limit exceeded beyond grace period.");
        }

        List<UserResponse> currentResponses = userResponseRepository.findByTestResultId(result.getId());
        double objectiveScore = 0.0;

        // ATOMIC GRADING
        for (UserResponse resp : currentResponses) {
            Question q = resp.getQuestion();
            double marks = calculateMarksFromEntity(q, resp);
            resp.setMarksObtained(marks);
            
            // Only auto-evaluate non-subjective questions
            if (q.getType() != QuestionType.SUBJECTIVE) {
                resp.setEvaluated(true);
                objectiveScore += marks;
            } else {
                resp.setEvaluated(false); // Flag for admin manual grading
            }
        }

        result.setObjectiveScore(objectiveScore);
        result.setTotalScore(objectiveScore); // Initial score (subjective starts at 0)
        result.setSubmittedAt(LocalDateTime.now());
        
        // STATUS BRIDGE
        boolean hasSubjective = currentResponses.stream()
                .anyMatch(r -> r.getQuestion().getType() == QuestionType.SUBJECTIVE);
        
        if (hasSubjective) {
            result.setStatus(ResultStatus.UNDER_EVALUATION);
            result.setFullyEvaluated(false);
        } else {
            result.setStatus(ResultStatus.COMPLETED);
            result.setFullyEvaluated(true);
        }

        return new TestResultResponse(testResultRepository.save(result));
    }

    /**
     * PRECISION GRADING ENGINE
     * Implements correct negative marking and NAT accuracy.
     */
   /**
     * PRECISION GRADING ENGINE
     * Implements correct negative marking, MSQ exact-match logic, and NAT tolerance.
     */
   private double calculateMarksFromEntity(Question q, UserResponse resp) {
    // 1. MCQ Logic (Single Correct)
    if (q.getType() == QuestionType.MCQ) {
        UUID correctId = q.getOptions().stream()
                // Use Lambda to avoid method reference ambiguity
                .filter(option -> option.isCorrect()) 
                .map(QuestionOption::getId)
                .findFirst()
                .orElse(null);
        
        if (resp.getSelectedOptionIds() != null && resp.getSelectedOptionIds().size() == 1 
            && resp.getSelectedOptionIds().contains(correctId)) {
            return q.getMarks();
        }
        return (resp.getSelectedOptionIds() == null || resp.getSelectedOptionIds().isEmpty()) 
                ? 0.0 : (q.getNegativeMarks() != null ? -q.getNegativeMarks() : 0.0);
    }

    // 2. MSQ Logic (Multiple Correct - All or Nothing)
    if (q.getType() == QuestionType.MSQ) {
        Set<UUID> correctIds = q.getOptions().stream()
                // Use Lambda here as well to fix the error
                .filter(option -> option.isCorrect()) 
                .map(QuestionOption::getId)
                .collect(Collectors.toSet());
        
        Set<UUID> userIds = resp.getSelectedOptionIds() != null ? 
                            new HashSet<>(resp.getSelectedOptionIds()) : Collections.emptySet();

        // Exact set match: User must pick ALL correct and ZERO incorrect options
        if (!userIds.isEmpty() && userIds.equals(correctIds)) {
            return q.getMarks();
        }
        return (userIds.isEmpty()) ? 0.0 : (q.getNegativeMarks() != null ? -q.getNegativeMarks() : 0.0);
    }

    // 3. NAT (Numerical Answer Type) Logic
    if (q.getType() == QuestionType.NAT) {
        if (q.getCorrectNumericalAnswer() != null && resp.getNumericalAnswer() != null) {
            // Delta check for floating point precision (0.001 tolerance)
            double delta = Math.abs(q.getCorrectNumericalAnswer() - resp.getNumericalAnswer());
            if (delta < 0.001) {
                return q.getMarks();
            }
        }
        return 0.0; 
    }

    return 0.0; 
}

    @Override
    @Transactional
    public void saveManualGrade(UUID responseId, Double assignedMarks) {
        UserResponse resp = userResponseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("Response record not found"));
        
        resp.setMarksObtained(assignedMarks);
        resp.setEvaluated(true);
        userResponseRepository.save(resp);

        TestResult result = resp.getTestResult();
        
        // Recalculate subjective total
        double totalSubj = result.getResponses().stream()
                .filter(r -> r.getQuestion().getType() == QuestionType.SUBJECTIVE)
                .mapToDouble(r -> r.getMarksObtained() != null ? r.getMarksObtained() : 0.0).sum();

        result.setSubjectiveScore(totalSubj);
        result.setTotalScore(result.getObjectiveScore() + totalSubj);

        // Auto-complete result if all questions are now evaluated
        if (result.getResponses().stream().allMatch(UserResponse::isEvaluated)) {
            result.setFullyEvaluated(true);
            result.setStatus(ResultStatus.COMPLETED);
        }
        testResultRepository.save(result);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionSummaryResponse getSummary(UUID resultId) {
        TestResult result = testResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        List<UserResponse> responses = result.getResponses();
        Exam exam = result.getExam();

        List<SubmissionSummaryResponse.SectionSummary> sectionSummaries = exam.getSections().stream()
            .map(section -> {
                int total = section.getQuestions().size();
                int answered = (int) responses.stream()
                    .filter(r -> r.getQuestion().getSection().getId().equals(section.getId()))
                    .filter(this::isAnswered)
                    .count();
                
                int marked = (int) responses.stream()
                    .filter(r -> r.getQuestion().getSection().getId().equals(section.getId()))
                    .filter(r -> "MARKED_FOR_REVIEW".equals(r.getResponseStatus()))
                    .count();

                return SubmissionSummaryResponse.SectionSummary.builder()
                    .sectionName(section.getSectionName())
                    .total(total)
                    .answered(answered)
                    .markedForReview(marked)
                    .build();
            }).collect(Collectors.toList());

        return SubmissionSummaryResponse.builder()
                .totalQuestions((int) exam.getSections().stream().flatMap(s -> s.getQuestions().stream()).count())
                .answeredCount((int) responses.stream().filter(this::isAnswered).count())
                .markedForReviewCount((int) responses.stream().filter(r -> "MARKED_FOR_REVIEW".equals(r.getResponseStatus())).count())
                .sections(sectionSummaries)
                .build();
    }

    private boolean isAnswered(UserResponse r) {
        return (r.getSelectedOptionIds() != null && !r.getSelectedOptionIds().isEmpty()) ||
               (r.getNumericalAnswer() != null) ||
               (r.getTextAnswer() != null && !r.getTextAnswer().isEmpty()) ||
               (r.getSketchData() != null);
    }
}