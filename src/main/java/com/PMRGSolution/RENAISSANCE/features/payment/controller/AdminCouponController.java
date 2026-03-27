package com.PMRGSolution.RENAISSANCE.features.payment.controller;

import com.PMRGSolution.RENAISSANCE.features.payment.dto.CouponRequest;
import com.PMRGSolution.RENAISSANCE.features.payment.dto.CouponResponse;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.Coupon;
import com.PMRGSolution.RENAISSANCE.features.payment.repository.CouponRepository;
import com.PMRGSolution.RENAISSANCE.features.payment.service.PaymentService;
import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> createCoupon(@RequestBody CouponRequest request) {
        paymentService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Coupon created successfully");
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(paymentService.getAllCoupons());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable UUID id) {
        paymentService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }
}