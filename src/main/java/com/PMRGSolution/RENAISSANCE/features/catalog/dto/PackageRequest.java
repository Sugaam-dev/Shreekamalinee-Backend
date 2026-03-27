package com.PMRGSolution.RENAISSANCE.features.catalog.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class PackageRequest {
    private String name;
    private BigDecimal priceAmount;
    private String tierType; // STARTER, STANDARD, PROFESSIONAL, UNIVERSAL_FREE
    private int durationInMonths; // 1, 3, 12
    
    // NEW: Define the limit for this specific package via the API
    private Integer mockTestLimit; 
    
    private List<UUID> categoryIds;
}