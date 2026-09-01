package com.pmrgsolution.features.cart.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponse {
    private UUID id;
    private UUID variantId;
    private String productName;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
}