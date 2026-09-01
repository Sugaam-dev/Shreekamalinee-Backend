package com.pmrgsolution.features.catalog.controller;

import com.pmrgsolution.features.catalog.dto.*;
import com.pmrgsolution.features.catalog.service.ProductService;
import com.pmrgsolution.features.catalog.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    // --- CATEGORY MANAGEMENT ---

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(productService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(productService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        productService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/categories/{id}/images")
    public ResponseEntity<CategoryResponse> uploadCategoryImage(
            @PathVariable UUID id,
            @RequestParam("image") MultipartFile file) {
        return ResponseEntity.ok(productService.uploadCategoryImage(id, file));
    }

    // --- PRODUCT MANAGEMENT ---

    @PostMapping("/products")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products/{id}/images")
    public ResponseEntity<ProductDTO> uploadProductImage(
            @PathVariable UUID id,
            @RequestParam("image") MultipartFile file) {
        return ResponseEntity.ok(productService.uploadProductImage(id, file));
    }

    @DeleteMapping("/products/{id}/images")
    public ResponseEntity<ProductDTO> deleteProductImage(
            @PathVariable UUID id,
            @RequestParam String imageUrl) {
        return ResponseEntity.ok(productService.deleteProductImage(id, imageUrl));
    }

    // --- VARIANT MANAGEMENT ---

    @PostMapping("/products/{id}/variants")
    public ResponseEntity<ProductVariantDTO> addVariant(@PathVariable UUID id, @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.ok(productService.addVariant(id, request));
    }

    @PutMapping("/variants/{variantId}/stock")
    public ResponseEntity<ProductVariantDTO> updateVariantStock(@PathVariable UUID variantId, @RequestParam Integer stock) {
        return ResponseEntity.ok(productService.updateVariantStock(variantId, stock));
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(@PathVariable UUID variantId) {
        productService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }

    // --- REVIEW MANAGEMENT & MODERATION ---

    @PostMapping("/products/{id}/reviews")
    public ResponseEntity<ReviewResponse> addAdminReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.addAdminReview(id, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReviewAdmin(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId, null, true);
        return ResponseEntity.noContent().build();
    }
}
