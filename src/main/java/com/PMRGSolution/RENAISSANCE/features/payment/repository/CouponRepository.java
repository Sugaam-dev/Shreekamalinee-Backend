package com.PMRGSolution.RENAISSANCE.features.payment.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.PMRGSolution.RENAISSANCE.features.payment.entity.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    
    // ✅ This matches 'couponCode' and 'active' fields
    Optional<Coupon> findByCouponCodeAndActiveTrue(String couponCode);

    // ✅ FIXED: Changed from existsByCode to existsByCouponCode 
    // to match the field 'couponCode' in your Entity
    boolean existsByCouponCode(String couponCode);
}