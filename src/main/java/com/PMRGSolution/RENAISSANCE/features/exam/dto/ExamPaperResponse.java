package com.PMRGSolution.RENAISSANCE.features.exam.dto;

import com.PMRGSolution.RENAISSANCE.Constant.QuestionType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamPaperResponse {
    // Session Info (Crucial for the Frontend Timer)
    private UUID sessionId;      // The TestResult ID
    private LocalDateTime startTime;
    private LocalDateTime expiryTime; // The absolute deadline
    
    // Exam Metadata
    private UUID examId;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Double totalMarks; // Useful for the progress bar
    private List<SectionDto> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionDto {
        private UUID sectionId;
        private String sectionName;
        private String sectionDescription;
        private List<QuestionDto> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private UUID questionId;
        private String content;
        private String imageUrl;
        private QuestionType type;
        private Double marks;
        private Double negativeMarks; // Frontend should show risk factor
        private List<OptionDto> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private UUID optionId;
        private String optionText;
        private Integer optionOrder; // Ensures options stay in A, B, C, D order
    }
}