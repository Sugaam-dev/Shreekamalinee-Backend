package com.PMRGSolution.RENAISSANCE.features.catalog.controller;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes; // FIXED: Using the new Enum
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.catalog.dto.*;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package;
import com.PMRGSolution.RENAISSANCE.features.catalog.service.CatalogService;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CatalogService catalogService;

    /**
     * UPLOAD: Optimized for the Unified Content approach.
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("title") String title, 
            @RequestPart("categoryId") String categoryId,
            @RequestPart("type") String type, // Will be parsed into ContentTypes
            @RequestPart("requiredTier") String requiredTier,
            @RequestPart(value = "displayOrder", required = false) String displayOrder,
            @RequestPart("file") MultipartFile file) throws IOException {
        
        if (file.isEmpty() || !file.getContentType().equalsIgnoreCase("application/pdf")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        // Convert Strings to correct Enums and UUIDs
        DocumentResponse response = catalogService.uploadDocument(
                title, 
                UUID.fromString(categoryId), 
                ContentTypes.valueOf(type), // FIXED: ContentTypes
                TierType.valueOf(requiredTier), 
                displayOrder != null ? Integer.parseInt(displayOrder) : 0, 
                file
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody ExamCategory category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.saveCategory(category));
    }

    @PostMapping("/packages")
    public ResponseEntity<Package> createPackage(@RequestBody PackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createPackage(request));
    }

    @PatchMapping("/documents/{id}")
    public ResponseEntity<DocumentResponse> updateMetadata(
            @PathVariable UUID id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) ContentTypes type, // FIXED: ContentTypes
            @RequestParam(required = false) TierType requiredTier,
            @RequestParam(required = false) Integer displayOrder) {
        
        DocumentResponse response = catalogService.updateDocumentMetadata(
                id, title, type, requiredTier, displayOrder
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        catalogService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}