package com.PMRGSolution.RENAISSANCE.features.exam.dto;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes;
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.Constant.QuestionType; // Ensure this is imported
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BulkExamRequest {
    private String title;
    private String description;
    private int durationMinutes;
    private UUID categoryId;
    private boolean published;
    private TierType requiredTier;
    private ContentTypes contentType; 
    private Integer displayOrder;
    private List<SectionRequest> sections;

    @Data
    public static class SectionRequest {
        private String name;
        private Integer order;
        private List<QuestionRequest> questions;
    }

    @Data
    public static class QuestionRequest {
        private String content;
        private QuestionType type; // FIXED: Changed from String to Enum
        private Double marks;
        private Double negativeMarks;
        private String explanation;
        private String imageUrl;
        private String correctNumericalAnswer;
        private List<OptionRequest> options;
    }

    @Data
    public static class OptionRequest {
        private String text;
        private Boolean isCorrect;
        private Integer order;
    }
}