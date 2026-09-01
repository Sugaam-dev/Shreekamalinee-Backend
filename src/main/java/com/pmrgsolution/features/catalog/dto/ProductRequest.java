package com.pmrgsolution.features.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotNull(message = "Original price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Original price must be greater than zero")
    private BigDecimal originalPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Offer price must be positive")
    private BigDecimal offerPrice;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,50}$", message = "SKU must be alphanumeric (hyphens/underscores allowed) and between 3-50 characters")
    private String sku;

    @NotBlank(message = "Gender category is required")
    @Pattern(regexp = "^(?i)(MEN|WOMEN|KIDS|UNISEX)$", message = "Gender category must be MEN, WOMEN, KIDS, or UNISEX")
    private String genderCategory; // e.g. "MEN", "WOMEN", "KIDS", "UNISEX"

    private String season;

    private String artisanalStory;

    private String fabricCare;

    private String shippingPolicy;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    private java.util.Map<String, String> highlights;

    private java.util.List<String> aboutItem;

    private java.util.List<VariantRequest> variants;
}
