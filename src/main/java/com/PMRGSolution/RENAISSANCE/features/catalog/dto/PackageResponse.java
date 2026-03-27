package com.PMRGSolution.RENAISSANCE.features.catalog.dto;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageResponse {
    private UUID id;
    private String name;
    private BigDecimal priceAmount;
    private Integer durationInMonths;
    private Integer mockTestLimit; // "You get 10 Mocks"
    private TierType tierType;     // STARTER, PROFESSIONAL, etc.
    
    // Show them exactly what they are unlocking
    private List<String> categoryNames; 
    private boolean isCombo; // True if more than 1 category
}