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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/coupons", "/api/v1/orders/coupons", "/api/orders/coupons"})
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final com.pmrgsolution.features.coupon.repository.CouponUsageRepository couponUsageRepository;

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAvailableCoupons(@AuthenticationPrincipal CustomUserDetails user) {
        String userEmail = user != null ? user.getUsername() : null;
        UUID userId = user != null ? user.getId() : null;
        LocalDateTime now = LocalDateTime.now();
        return ResponseEntity.ok(couponService.getAllCoupons().stream()
                .filter(c -> {
                    if (c.getApplicableUserEmails() != null && !c.getApplicableUserEmails().isEmpty()) {
                        return userEmail != null && c.getApplicableUserEmails().stream()
                                .anyMatch(email -> email != null && email.equalsIgnoreCase(userEmail));
                    }
                    return true;
                })
                .peek(c -> {
                    boolean expired = !c.isActive() || (c.getExpiryDate() != null && c.getExpiryDate().isBefore(now));
                    c.setIsExpired(expired);
                    if (userId != null) {
                        boolean used = couponUsageRepository.countByCouponIdAndUserId(c.getId(), userId) > 0;
                        c.setIsUsedByUser(used);
                    } else {
                        c.setIsUsedByUser(false);
                    }
                })
                .collect(Collectors.toList()));
    }

    @GetMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestParam("code") String code,
            @RequestParam(value = "subtotal", required = false) BigDecimal subtotal,
            @AuthenticationPrincipal CustomUserDetails user) {
        UUID userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(couponService.validateCoupon(code, subtotal, userId));
    }
}