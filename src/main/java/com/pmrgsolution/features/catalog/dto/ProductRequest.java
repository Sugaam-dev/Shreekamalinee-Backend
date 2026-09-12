package com.pmrgsolution.features.catalog.dto;

import com.pmrgsolution.constant.GenderCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    private String description;
    private String brand;

    @NotNull(message = "Original price is required")
    private BigDecimal originalPrice;

    private BigDecimal offerPrice;

    @NotBlank(message = "SKU is required")
    private String sku;

    private GenderCategory genderCategory;
    private String season;
    private String artisanalStory;
    private String fabricCare;
    private String shippingPolicy;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private Map<String, String> highlights;
    private List<String> aboutItem;
    private List<String> imageUrls;
    private List<@Valid VariantRequest> variants;
}