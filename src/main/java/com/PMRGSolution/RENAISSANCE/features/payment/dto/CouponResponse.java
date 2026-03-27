package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class CouponResponse {
    private UUID id;
    private String code;
    private String discountType; // PERCENTAGE or FIXED
    private Double discountValue;
    private Double minPurchaseAmount;
    private LocalDateTime expiryDate;
    private boolean active;
    private Set<String> applicableCategoryNames; // Helpful for the UI
}