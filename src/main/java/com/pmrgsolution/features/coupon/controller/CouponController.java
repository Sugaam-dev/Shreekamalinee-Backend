package com.pmrgsolution.features.coupon.controller;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.coupon.dto.CouponResponse;
import com.pmrgsolution.features.coupon.dto.CouponValidationResponse;
import com.pmrgsolution.features.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/coupons", "/api/v1/orders/coupons", "/api/orders/coupons"})
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAvailableCoupons(@AuthenticationPrincipal CustomUserDetails user) {
        String userEmail = user != null ? user.getUsername() : null;
        UUID userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(couponService.getAvailableCouponsForUser(userId, userEmail));
    }

    @GetMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestParam("code") String code,
            @RequestParam(value = "subtotal", required = false) BigDecimal subtotal,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "email", required = false) String email,
            @AuthenticationPrincipal CustomUserDetails user) {
        String targetEmail = userEmail != null && !userEmail.isBlank() ? userEmail : email;
        UUID userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(couponService.validateCoupon(code, subtotal, userId, targetEmail));
    }
}