package com.pmrgsolution.features.catalog.service;

import com.pmrgsolution.exception.BusinessException;
import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.catalog.dto.ProductReviewsSummaryDTO;
import com.pmrgsolution.features.catalog.dto.ReviewRequest;
import com.pmrgsolution.features.catalog.dto.ReviewResponse;
import com.pmrgsolution.features.catalog.entity.Product;
import com.pmrgsolution.features.catalog.entity.Review;
import com.pmrgsolution.features.catalog.repository.ProductRepository;
import com.pmrgsolution.features.catalog.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final com.pmrgsolution.core.service.RealtimeEventService realtimeEventService;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "reviews", key = "#productId"),
        @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    })
    public ReviewResponse addOrUpdateReview(UUID productId, UUID userId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> Review.builder()
                        .product(product)
                        .user(user)
                        .build());

        review.setRating(request.getRating());
        review.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        review.setComment(request.getComment().trim());

        Review saved = reviewRepository.save(review);
        log.info("Review saved for product '{}' by user '{}' with rating {}", productId, user.getEmail(), request.getRating());
        realtimeEventService.broadcast("REVIEW_UPDATED", "{\"type\":\"REVIEW_UPDATED\",\"productId\":\"" + productId + "\"}");
        realtimeEventService.broadcast("PRODUCT_UPDATED", "{\"type\":\"PRODUCT_UPDATED\",\"productId\":\"" + productId + "\"}");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "reviews", key = "#productId"),
        @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    })
    public ReviewResponse addAdminReview(UUID productId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String reviewerName = request.getReviewerName() != null && !request.getReviewerName().isBlank()
                ? request.getReviewerName().trim()
                : "Verified Customer";

        Review review = Review.builder()
                .product(product)
                .user(null)
                .reviewerName(reviewerName)
                .rating(request.getRating())
                .title(request.getTitle() != null ? request.getTitle().trim() : null)
                .comment(request.getComment().trim())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Admin added verified testimonial review for product '{}' by '{}'", productId, reviewerName);
        realtimeEventService.broadcast("REVIEW_UPDATED", "{\"type\":\"REVIEW_UPDATED\",\"productId\":\"" + productId + "\"}");
        realtimeEventService.broadcast("PRODUCT_UPDATED", "{\"type\":\"PRODUCT_UPDATED\",\"productId\":\"" + productId + "\"}");
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "#productId")
    public ProductReviewsSummaryDTO getProductReviews(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }

        List<Review> reviews = reviewRepository.findByProductIdWithUser(productId);
        Double rawAvg = reviewRepository.getAverageRating(productId);
        double roundedAvg = BigDecimal.valueOf(rawAvg != null ? rawAvg : 0.0)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        List<ReviewResponse> mappedList = reviews.stream()
                .map(this::mapToResponse)
                .toList();

        return ProductReviewsSummaryDTO.builder()
                .productId(productId)
                .averageRating(roundedAvg)
                .totalReviews((long) reviews.size())
                .reviews(mappedList)
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "reviews", allEntries = true),
        @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    })
    public void deleteReview(UUID reviewId, UUID userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!isAdmin && (review.getUser() == null || !review.getUser().getId().equals(userId))) {
            throw new BusinessException("You are not authorized to delete this review", HttpStatus.FORBIDDEN);
        }

        UUID productId = review.getProduct() != null ? review.getProduct().getId() : null;
        reviewRepository.delete(review);
        log.info("Review '{}' deleted by user '{}' (isAdmin: {})", reviewId, userId, isAdmin);
        if (productId != null) {
            realtimeEventService.broadcast("REVIEW_UPDATED", "{\"type\":\"REVIEW_UPDATED\",\"productId\":\"" + productId + "\"}");
            realtimeEventService.broadcast("PRODUCT_UPDATED", "{\"type\":\"PRODUCT_UPDATED\",\"productId\":\"" + productId + "\"}");
        }
    }

    private ReviewResponse mapToResponse(Review review) {
        String name = "Customer";
        if (review.getReviewerName() != null && !review.getReviewerName().isBlank()) {
            name = review.getReviewerName();
        } else if (review.getUser() != null) {
            String firstName = review.getUser().getFirstName() != null ? review.getUser().getFirstName() : "";
            String lastName = review.getUser().getLastName() != null ? review.getUser().getLastName() : "";
            String fullName = (firstName + " " + lastName).trim();
            name = !fullName.isBlank() ? fullName : review.getUser().getEmail().split("@")[0];
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(name)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
