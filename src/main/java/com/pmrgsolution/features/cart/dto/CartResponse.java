package com.pmrgsolution.features.cart.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {
    private UUID id;
    private List<CartItemDTO> items;
    private BigDecimal totalAmount;
}