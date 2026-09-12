package com.pmrgsolution.features.coupon.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageResponse {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;
    private UUID orderId;
    private String orderNumber;
    private BigDecimal discountAmount;
    private BigDecimal orderAmount;
    private LocalDateTime usedAt;
}
