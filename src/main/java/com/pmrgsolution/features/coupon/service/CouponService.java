package com.pmrgsolution.features.coupon.service;

import com.pmrgsolution.features.coupon.dto.CouponRequest;
import com.pmrgsolution.features.coupon.dto.CouponResponse;
import com.pmrgsolution.features.coupon.dto.CouponUsageResponse;
import com.pmrgsolution.features.coupon.dto.CouponValidationResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CouponService {
    void createCoupon(CouponRequest request);
    List<CouponResponse> getAllCoupons();
    void deleteCoupon(UUID id);
    CouponValidationResponse validateCoupon(String code, BigDecimal subtotal, UUID userId);
    CouponValidationResponse validateCoupon(String code, BigDecimal subtotal, UUID userId, String userEmail);
    List<CouponUsageResponse> getCouponUsages(UUID couponId);
}