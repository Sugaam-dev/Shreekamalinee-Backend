package com.pmrgsolution.features.coupon.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private UUID id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal minPurchaseAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private int timesUsed;
    private LocalDateTime expiryDate;
    private boolean isActive;
    private Boolean active;
    private Boolean isUsedByUser;
    private Boolean isExpired;
    private List<String> applicableUserEmails;
    private LocalDateTime createdAt;

    public boolean isActive() {
        return isActive;
    }

    public BigDecimal getMinPurchaseAmount() {
        return minPurchaseAmount != null ? minPurchaseAmount : minOrderAmount;
    }

    public Boolean getActive() {
        return active != null ? active : isActive;
    }
}