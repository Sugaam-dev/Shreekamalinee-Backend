package com.pmrgsolution.features.order.dto;

import com.pmrgsolution.features.catalog.dto.ProductDTO;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String productSku;
    private String imageUrl;
    private UUID variantId;
    private String size;
    private String color;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private ProductDTO product;
}