package com.pmrgsolution.features.catalog.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewsSummaryDTO {
    private UUID productId;
    private Double averageRating;
    private Long totalReviews;
    private List<ReviewResponse> reviews;
}
