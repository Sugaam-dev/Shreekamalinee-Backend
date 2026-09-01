package com.pmrgsolution.features.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    @Pattern(regexp = "^$|^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @JsonAlias({"parentCategoryId", "parentId", "parent_id"})
    private UUID parentId;

    @JsonAlias({"imageUrl", "image_url"})
    private String imageUrl;

    @JsonAlias({"suggestedAttributes", "suggested_attributes", "attributes"})
    private List<String> suggestedAttributes;
}
