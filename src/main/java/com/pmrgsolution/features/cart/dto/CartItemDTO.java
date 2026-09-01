package com.pmrgsolution.features.cart.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO {
    private UUID id;
    private UUID variantId;
    private String productName;
    private String size;
    private String color;
    private BigDecimal price; // Active unit price (offerPrice or originalPrice)
    private Integer quantity;
    private String imageUrl;
}
