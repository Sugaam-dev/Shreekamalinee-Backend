package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
}