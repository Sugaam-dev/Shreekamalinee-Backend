package com.pmrgsolution.features.catalog.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductReviewsSummaryResponse {
    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> starCounts;
}