package com.pmrgsolution.features.catalog.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantDTO {
    private UUID id;
    private String size;
    private String color;
    private Integer stockQuantity;
    private String sku;
}
