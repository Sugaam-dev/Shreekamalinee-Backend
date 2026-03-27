package com.PMRGSolution.RENAISSANCE.features.catalog.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String displayName;
    private String description;
    
    // Professional Logic Fields
    private boolean isSubscribed; // Tells React to show Lock or Unlock icon
    private Long daysRemaining;   // Null if locked, number if unlocked
}