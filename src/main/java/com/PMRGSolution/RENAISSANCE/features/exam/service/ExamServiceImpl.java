package com.PMRGSolution.RENAISSANCE.features.exam.service;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes;
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.Exception.BusinessException;
import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import com.PMRGSolution.RENAISSANCE.features.exam.dto.*;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.*;
import com.PMRGSolution.RENAISSANCE.features.exam.repository.*;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.UserSubscription;
import com.PMRGSolution.RENAISSANCE.features.payment.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamCategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final TestSectionRepository sectionRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final TestResultRepository testResultRepository;

    // --- STUDENT ACCESS ---

    @Override
    @Transactional(readOnly = true)
    public List<ExamListResponse> getExamsForUserByCategory(UUID categoryId, String userEmail) {
        UserSubscription sub = subscriptionRepository.findActiveSubscription(userEmail, categoryId, LocalDateTime.now()).orElse(null);
        TierType userTier = (sub != null) ? sub.getTier() : TierType.UNIVERSAL_FREE;

        return examRepository.findByCategoryId(categoryId).stream()
                .filter(Exam::isPublished)
                .map(exam -> ExamListResponse.builder()
                        .id(exam.getId())
                        .title(exam.getTitle())
                        .totalMarks(exam.getTotalMarks())
                        .durationMinutes(exam.getDurationMinutes())
                        .examType(exam.getContentType().name()) 
                        .requiredTier(exam.getRequiredTier())
                        .isLocked(userTier.getRank() < exam.getRequiredTier().getRank())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamPaperResponse getExamPaper(UUID examId, String userEmail) {
        validateExamAccess(examId, userEmail);
        Exam exam = getExamById(examId);
        return ExamPaperResponse.builder()
                .examId(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .durationMinutes(exam.getDurationMinutes())
                .totalMarks(exam.getTotalMarks())
                .sections(exam.getSections().stream()
                        .map(this::mapToSectionDto)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public void validateExamAccess(UUID examId, String userEmail) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        if (exam.getRequiredTier() == TierType.UNIVERSAL_FREE) return;

        UserSubscription sub = subscriptionRepository.findActiveSubscription(userEmail, exam.getCategory().getId(), LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Active subscription required"));

        if (sub.getTier().getRank() < exam.getRequiredTier().getRank()) {
            throw new BusinessException("Upgrade required to unlock this exam.");
        }

        if (exam.getContentType() == ContentTypes.MOCK_TEST) {
            int limit = sub.getProductPackage().getMockTestLimit();
            long count = testResultRepository.countUniqueExamsStartedInCategory(userEmail, exam.getCategory().getId());
            boolean exists = testResultRepository.existsByUserEmailAndExamId(userEmail, examId);
            if (!exists && count >= limit) throw new BusinessException("Mock test limit reached.", HttpStatus.PAYMENT_REQUIRED);
        }
    }

    // --- ADMIN: BULK UPLOAD ---

    @Override
    @Transactional
    public UUID saveFullExam(BulkExamRequest request) {
        ExamCategory cat = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .contentType(request.getContentType()) 
                .category(cat)
                .published(request.isPublished())
                .active(true)
                .requiredTier(request.getRequiredTier() != null ? request.getRequiredTier() : TierType.UNIVERSAL_FREE)
                // PROPER SOLUTION: Saving display order from Admin DTO
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .sections(new ArrayList<>())
                .build();

        List<TestSection> sections = IntStream.range(0, request.getSections().size()).mapToObj(i -> {
            BulkExamRequest.SectionRequest sDto = request.getSections().get(i);
            TestSection section = TestSection.builder()
                    .sectionName(sDto.getName())
                    .sectionOrder(sDto.getOrder() != null ? sDto.getOrder() : i + 1)
                    .exam(exam) 
                    .questions(new ArrayList<>())
                    .build();

            section.setQuestions(sDto.getQuestions().stream()
                    .map(qDto -> createQuestionEntity(qDto, section))
                    .collect(Collectors.toList()));
            return section;
        }).collect(Collectors.toList());

        exam.setSections(sections);
        Exam saved = examRepository.save(exam);
        updateExamTotalMarks(saved.getId());
        return saved.getId();
    }

    // --- GRANULAR ENTITY CREATION ---

    private Question createQuestionEntity(BulkExamRequest.QuestionRequest dto, TestSection section) {
        Question q = Question.builder()
                .content(dto.getContent())
                .type(dto.getType()) 
                .marks(dto.getMarks())
                .negativeMarks(dto.getNegativeMarks() != null ? dto.getNegativeMarks() : 0.0)
                .explanation(dto.getExplanation())
                .imageUrl(dto.getImageUrl())
                .correctNumericalAnswer(dto.getCorrectNumericalAnswer() != null ? Double.valueOf(dto.getCorrectNumericalAnswer()) : null)
                .section(section)
                .options(new ArrayList<>())
                .build();

        if (dto.getOptions() != null) {
            // FIXED: Using type witness and explicit return for stream stability
            List<QuestionOption> options = dto.getOptions().stream()
                    .<QuestionOption>map(o -> QuestionOption.builder()
                            .optionText(o.getText())
                            .isCorrect(o.getIsCorrect() != null ? o.getIsCorrect() : false)
                            .optionOrder(o.getOrder())
                            .question(q)
                            .build())
                    .collect(Collectors.toList());
            q.setOptions(options);
        }
        return q;
    }

    @Override 
    @Transactional
    public void updateExamTotalMarks(UUID examId) {
        Exam exam = examRepository.findById(examId).orElseThrow();
        double sum = exam.getSections().stream()
                .flatMap(s -> s.getQuestions().stream())
                .mapToDouble(q -> q.getMarks() != null ? q.getMarks() : 0.0)
                .sum();
        exam.setTotalMarks(sum);
        examRepository.save(exam);
    }

    @Override 
    @Transactional
    public Question updateQuestion(UUID questionId, BulkExamRequest.QuestionRequest dto) { 
        Question q = questionRepository.findById(questionId).orElseThrow();
        q.setContent(dto.getContent());
        q.setType(dto.getType());
        q.setMarks(dto.getMarks());
        q.setNegativeMarks(dto.getNegativeMarks() != null ? dto.getNegativeMarks() : 0.0);
        q.setExplanation(dto.getExplanation());
        q.setImageUrl(dto.getImageUrl());
        
        if (dto.getOptions() != null) {
            q.getOptions().clear();
            // FIXED: Re-mapping with Type Witness
            List<QuestionOption> updatedOptions = dto.getOptions().stream()
                    .<QuestionOption>map(o -> QuestionOption.builder()
                            .optionText(o.getText())
                            .isCorrect(o.getIsCorrect() != null ? o.getIsCorrect() : false)
                            .optionOrder(o.getOrder())
                            .question(q)
                            .build())
                    .collect(Collectors.toList());
            
            q.getOptions().addAll(updatedOptions);
        }
        
        Question updated = questionRepository.save(q);
        updateExamTotalMarks(q.getSection().getExam().getId());
        return updated;
    }

    // --- OTHER METHODS ---

    @Override 
    @Transactional
    public Question addQuestionToSection(UUID sectionId, BulkExamRequest.QuestionRequest dto) { 
        TestSection section = sectionRepository.findById(sectionId).orElseThrow();
        Question q = createQuestionEntity(dto, section);
        q.setQuestionOrder(section.getQuestions().size() + 1);
        Question saved = questionRepository.save(q);
        updateExamTotalMarks(section.getExam().getId());
        return saved;
    }

    @Override @Transactional public void deleteQuestion(UUID qId) { 
        Question q = questionRepository.findById(qId).orElseThrow();
        UUID exId = q.getSection().getExam().getId();
        questionRepository.delete(q);
        updateExamTotalMarks(exId);
    }

    private ExamPaperResponse.SectionDto mapToSectionDto(TestSection s) {
        return ExamPaperResponse.SectionDto.builder()
                .sectionId(s.getId()).sectionName(s.getSectionName())
                .questions(s.getQuestions().stream().map(this::mapToQuestionDto).collect(Collectors.toList()))
                .build();
    }

    private ExamPaperResponse.QuestionDto mapToQuestionDto(Question q) {
        return ExamPaperResponse.QuestionDto.builder()
                .questionId(q.getId()).content(q.getContent()).type(q.getType()).imageUrl(q.getImageUrl())
                .options(q.getOptions().stream().map(o -> ExamPaperResponse.OptionDto.builder()
                .optionId(o.getId()).optionText(o.getOptionText()).build()).collect(Collectors.toList())).build();
    }

    @Override public Exam getExamById(UUID id) { return examRepository.findById(id).orElseThrow(); }
    @Override public void deleteExam(UUID id) { examRepository.deleteById(id); }
    @Override public List<Exam> getAllPublishedExams() { return examRepository.findByActiveTrueAndPublishedTrue(); }
    @Override public List<Exam> getExamsByCategory(UUID id) { return examRepository.findByCategoryId(id); }
    @Override public List<Exam> getAllActiveExams() { return examRepository.findByActiveTrue(); }
    @Override @Transactional public void togglePublishStatus(UUID id, boolean status) { Exam e = getExamById(id); e.setPublished(status); examRepository.save(e); }
}