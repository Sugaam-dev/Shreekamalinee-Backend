package com.PMRGSolution.RENAISSANCE.features.exam.dto;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.TestResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResultResponse {
    private UUID resultId;
    private String examTitle;
    private Double objectiveScore;
    private Double subjectiveScore;
    private Double totalScore;
    private Double totalMarksPossible; // Added: For percentage calc
    private String status;
    private boolean fullyEvaluated; // Added: To show "Results Pending" UI

    public TestResultResponse(TestResult result) {
        if (result != null) {
            this.resultId = result.getId();
            this.examTitle = (result.getExam() != null) ? result.getExam().getTitle() : "Unknown Exam";
            this.objectiveScore = result.getObjectiveScore();
            this.subjectiveScore = result.getSubjectiveScore();
            this.totalScore = result.getTotalScore();
            this.totalMarksPossible = result.getTotalMarksPossible();
            this.status = result.getStatus().name();
            this.fullyEvaluated = result.isFullyEvaluated();
        }
    }
}