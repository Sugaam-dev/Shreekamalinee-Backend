package com.pmrgsolution.features.coupon.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidationResponse {
    private boolean valid;
    private String message;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal calculatedDiscount;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
}