package com.pmrgsolution.features.coupon.repository;

import com.pmrgsolution.features.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    /**
     * SECURITY FIX: Atomically increments timesUsed at DB level.
     * Replaces the non-atomic read-modify-write pattern that caused a race condition
     * allowing coupons to be used more times than their usageLimit.
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.timesUsed = c.timesUsed + 1 WHERE c.id = :id")
    void incrementTimesUsed(@Param("id") UUID id);
}