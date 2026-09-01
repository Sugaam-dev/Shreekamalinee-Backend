package com.pmrgsolution.features.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "coupons",
    indexes = {
        @Index(name = "idx_coupon_code", columnList = "coupon_code", unique = true),
        @Index(name = "idx_coupon_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "coupon_code", unique = true)
    private String code;

    @Column(name = "discount_type")
    private String discountType; // PERCENTAGE, FIXED

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_purchase_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "times_used")
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "description")
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "coupon_applicable_users", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "user_email")
    @Builder.Default
    private List<String> applicableUserEmails = new ArrayList<>();

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponUsage> usages;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getCouponCode() {
        return code;
    }

    public void setCouponCode(String couponCode) {
        this.code = couponCode;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    public int getTimesUsed() {
        return this.timesUsed != null ? this.timesUsed : 0;
    }
}