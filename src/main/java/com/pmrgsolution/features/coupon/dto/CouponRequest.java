package com.pmrgsolution.features.coupon.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRequest {
    private String code;
    private String discountType; // PERCENTAGE, FIXED
    private BigDecimal discountValue;

    @JsonAlias({"minPurchaseAmount", "minimumPurchaseAmount", "minAmount"})
    private BigDecimal minOrderAmount;

    @JsonAlias({"maxDiscount", "maximumDiscountAmount"})
    private BigDecimal maxDiscountAmount;

    private Integer usageLimit;
    private LocalDateTime expiryDate;

    @JsonAlias({"active", "enabled"})
    @JsonProperty("isActive")
    private Boolean isActive;

    private List<String> applicableUserEmails;

    public void setMinPurchaseAmount(BigDecimal minPurchaseAmount) {
        if (this.minOrderAmount == null) {
            this.minOrderAmount = minPurchaseAmount;
        }
    }

    public void setActive(Boolean active) {
        if (this.isActive == null) {
            this.isActive = active;
        }
    }
}