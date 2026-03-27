package com.PMRGSolution.RENAISSANCE.features.exam.controller;

import com.PMRGSolution.RENAISSANCE.features.catalog.dto.CategoryResponse;
import com.PMRGSolution.RENAISSANCE.features.catalog.service.CatalogService;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import com.PMRGSolution.RENAISSANCE.features.exam.repository.ExamCategoryRepository;
import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CatalogService catalogService;
    private final ExamCategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody ExamCategory category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.saveCategory(category));
    }

    @GetMapping
    public ResponseEntity<List<ExamCategory>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamCategory> updateCategory(@PathVariable UUID id, @RequestBody ExamCategory details) {
        ExamCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        cat.setDisplayName(details.getDisplayName());
        cat.setDescription(details.getDescription());
        
        return ResponseEntity.ok(categoryRepository.save(cat));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        if (!categoryRepository.existsById(id)) throw new ResourceNotFoundException("Category not found");
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}