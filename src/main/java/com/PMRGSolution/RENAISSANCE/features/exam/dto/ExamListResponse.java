package com.PMRGSolution.RENAISSANCE.features.exam.dto;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamListResponse {
    private UUID id;
    private String title;
    private String description;
    private Double totalMarks;
    private Integer durationMinutes;
    
    // FIX: Changed from DocumentType to String to match Entity
    private String examType;
    
    private TierType requiredTier;
    private boolean isLocked; 
}