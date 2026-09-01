package com.pmrgsolution.features.catalog.controller;

import com.pmrgsolution.features.catalog.dto.CategoryResponse;
import com.pmrgsolution.features.catalog.dto.ProductDTO;
import com.pmrgsolution.features.catalog.dto.ProductReviewsSummaryDTO;
import com.pmrgsolution.features.catalog.dto.ReviewRequest;
import com.pmrgsolution.features.catalog.dto.ReviewResponse;
import com.pmrgsolution.features.catalog.service.ProductService;
import com.pmrgsolution.features.catalog.service.ReviewService;
import com.pmrgsolution.core.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    // --- CATEGORIES & CAROUSELS ---

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @GetMapping("/categories/main")
    public ResponseEntity<List<CategoryResponse>> getMainCategories() {
        return ResponseEntity.ok(productService.getMainCategories());
    }

    @GetMapping("/categories/subcategories")
    public ResponseEntity<List<CategoryResponse>> getSubcategories(
            @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(productService.getSubcategories(parentId));
    }

    // --- PRODUCTS CATALOG, SEARCH & ADVANCED MULTI-FILTERING ---

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) String sortBy) {
        return ResponseEntity.ok(productService.getProducts(categoryId, gender, brand, search, season, minPrice, maxPrice, inStock, minRating, minDiscount, sortBy));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // --- PRODUCT REVIEWS & STAR RATINGS ---

    @PostMapping("/products/{id}/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.addOrUpdateReview(id, user.getId(), request));
    }

    @GetMapping("/products/{id}/reviews")
    public ResponseEntity<ProductReviewsSummaryDTO> getProductReviews(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.getProductReviews(id));
    }

    @DeleteMapping("/products/{id}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID id,
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));
        reviewService.deleteReview(reviewId, user.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
