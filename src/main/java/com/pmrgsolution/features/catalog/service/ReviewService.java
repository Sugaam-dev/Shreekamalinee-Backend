package com.pmrgsolution.features.catalog.service;

import com.pmrgsolution.features.catalog.dto.ProductReviewsSummaryDTO;
import com.pmrgsolution.features.catalog.dto.ReviewRequest;
import com.pmrgsolution.features.catalog.dto.ReviewResponse;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse addOrUpdateReview(UUID productId, UUID userId, ReviewRequest request);
    ReviewResponse addAdminReview(UUID productId, ReviewRequest request);
    ProductReviewsSummaryDTO getProductReviews(UUID productId);
    void deleteReview(UUID reviewId, UUID userId, boolean isAdmin);
}
