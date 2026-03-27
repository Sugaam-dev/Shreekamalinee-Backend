package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class CouponRequest {
    private String code;
    private String discountType; // "PERCENTAGE" or "FIXED"
    private Double discountValue;
    private Double minPurchaseAmount;
    private LocalDateTime expiryDate;
    private boolean active;
    private Set<UUID> applicableCategoryIds; // Optional: restrict to specific exams
}