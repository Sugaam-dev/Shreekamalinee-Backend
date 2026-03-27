package com.PMRGSolution.RENAISSANCE.features.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "coupon_code", unique = true, nullable = false, length = 50)
    private String couponCode;

    // ✅ FIXED: Added discountType to handle PERCENTAGE vs FIXED
    @Column(name = "discount_type", nullable = false)
    @Builder.Default
    private String discountType = "FIXED"; 

    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    // ✅ FIXED: Added minPurchaseAmount to handle validation logic
    @Column(name = "min_purchase_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
}