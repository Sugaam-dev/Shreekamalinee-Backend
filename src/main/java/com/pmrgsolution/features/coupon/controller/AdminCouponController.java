package com.pmrgsolution.features.coupon.controller;

import com.pmrgsolution.features.coupon.dto.CouponRequest;
import com.pmrgsolution.features.coupon.dto.CouponResponse;
import com.pmrgsolution.features.coupon.dto.CouponUsageResponse;
import com.pmrgsolution.features.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/coupons", "/api/admin/coupons"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<String> createCoupon(@RequestBody CouponRequest request) {
        couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Coupon created successfully");
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<List<CouponUsageResponse>> getCouponUsages(@PathVariable UUID id) {
        return ResponseEntity.ok(couponService.getCouponUsages(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable UUID id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }
}