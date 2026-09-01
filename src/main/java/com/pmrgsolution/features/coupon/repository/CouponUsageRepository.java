package com.pmrgsolution.features.coupon.repository;

import com.pmrgsolution.features.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    long countByCouponIdAndUserId(UUID couponId, UUID userId);
    long countByCouponId(UUID couponId);
    List<CouponUsage> findByCouponIdOrderByUsedAtDesc(UUID couponId);
}