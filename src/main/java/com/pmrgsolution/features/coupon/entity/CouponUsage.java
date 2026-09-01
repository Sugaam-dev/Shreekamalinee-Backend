package com.pmrgsolution.features.coupon.entity;

import com.pmrgsolution.features.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "coupon_usages",
    indexes = {
        @Index(name = "idx_coupon_usage_user", columnList = "user_id"),
        @Index(name = "idx_coupon_usage_coupon", columnList = "coupon_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_id")
    private UUID orderId;

    @CreationTimestamp
    private LocalDateTime usedAt;
}