package com.pmrgsolution.features.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    
    private UUID id;

    @NotBlank(message = "Category name is required")
    private String name;

    private String slug;

    @JsonAlias({"parentCategoryId", "parentId", "parent_id"})
    private UUID parentId;

    private String parentCategoryName;

    @JsonAlias({"imageUrl", "image_url"})
    private String imageUrl;

    private List<String> suggestedAttributes;
}