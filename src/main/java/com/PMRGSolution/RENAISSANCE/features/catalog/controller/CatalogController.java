package com.PMRGSolution.RENAISSANCE.features.catalog.controller;

import com.PMRGSolution.RENAISSANCE.features.catalog.dto.CategoryResponse; // Fixed import to match the updated DTO package
import com.PMRGSolution.RENAISSANCE.features.catalog.dto.DocumentResponse;
import com.PMRGSolution.RENAISSANCE.features.catalog.dto.PackageResponse;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package;
import com.PMRGSolution.RENAISSANCE.features.catalog.service.CatalogService;
import com.PMRGSolution.RENAISSANCE.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * DASHBOARD API: Returns all exams with Locked/Unlocked status and Days Remaining.
     * This is the "Automatic" entry point for the frontend.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllExams(@AuthenticationPrincipal CustomUserDetails user) {
        // user.getId() is passed to calculate isSubscribed and daysRemaining specifically for them
        return ResponseEntity.ok(catalogService.getExamsForUser(user.getId()));
    }
    
    @GetMapping("/packages/filter")
    public ResponseEntity<List<PackageResponse>> getFilteredPackages(@RequestParam UUID categoryId) {
        return ResponseEntity.ok(catalogService.getPackagesByCategoryId(categoryId));
    }

    /**
     * CONTENT API: Returns materials for a specific exam.
     * Security: Logic inside service blocks non-subscribers from seeing non-sample files.
     */
    @GetMapping("/exams/{examId}/materials")
    public ResponseEntity<List<DocumentResponse>> getMaterials(
            @PathVariable UUID examId, 
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(catalogService.getDocumentsByExam(examId, user.getId()));
    }

    /**
     * SECURITY API: Generates a temporary, signed link for a PDF.
     */
    @GetMapping("/documents/{docId}/access")
    public ResponseEntity<String> getSecureLink(
            @PathVariable UUID docId, 
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(catalogService.generateSecureViewLink(docId, user.getId()));
    }

    /**
     * STREAMING API: Serves the watermarked PDF.
     */
    @GetMapping("/stream/{docId}")
    public ResponseEntity<byte[]> stream(
            @PathVariable UUID docId, 
            @RequestParam String token, 
            @RequestParam long expires, 
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {
        
        // Token Expiry Validation
        if (System.currentTimeMillis() > expires) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] pdf = catalogService.getWatermarkedStream(docId, user.getId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("renaissance_material.pdf").build());
        headers.setCacheControl("no-cache, no-store");
        
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /**
     * MARKETING API: Get all subscription packages (Price plans).
     */
    @GetMapping("/packages")
    public ResponseEntity<List<Package>> getPackages() {
        return ResponseEntity.ok(catalogService.getAllPackages());
    }
}