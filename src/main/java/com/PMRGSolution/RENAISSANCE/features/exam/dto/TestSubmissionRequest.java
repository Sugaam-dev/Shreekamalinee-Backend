package com.PMRGSolution.RENAISSANCE.features.exam.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class TestSubmissionRequest {
    private UUID examId;
    private List<AnswerDto> answers;

    @Data
    public static class AnswerDto {
        private UUID questionId;
        private List<UUID> selectedOptionIds;
        private Double numericalAnswer;
        private String textAnswer;
        private String sketchData; // Base64
        private String responseStatus; // Added: SAVED or MARKED_FOR_REVIEW
    }
}