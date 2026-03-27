package com.PMRGSolution.RENAISSANCE.features.exam.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SubmissionSummaryResponse {
    private int totalQuestions;
    private int answeredCount;
    private int markedForReviewCount;
    private int unvisitedCount;
    private List<SectionSummary> sections;

    @Data
    @Builder
    public static class SectionSummary {
        private String sectionName;
        private int answered;
        private int markedForReview;
        private int total;
    }
}