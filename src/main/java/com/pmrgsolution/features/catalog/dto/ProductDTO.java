package com.pmrgsolution.features.catalog.dto;

import com.pmrgsolution.constant.GenderCategory;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private UUID id;
    private String name;
    private String description;
    private String brand;
    private BigDecimal originalPrice;
    private BigDecimal offerPrice;
    private String sku;
    private GenderCategory genderCategory;
    private String season;
    private String artisanalStory;
    private String fabricCare;
    private String shippingPolicy;
    private UUID categoryId;
    private String categoryName;
    private String parentCategoryName;
    private Integer discountPercentage;
    private Boolean inStock;
    private Integer totalStock;
    private Double averageRating;
    private Long totalReviews;
    private java.util.Map<String, String> highlights;
    private List<String> aboutItem;
    private List<String> imageUrls;
    private List<ProductVariantDTO> variants;
}