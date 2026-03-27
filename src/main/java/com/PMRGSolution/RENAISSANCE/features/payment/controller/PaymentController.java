package com.PMRGSolution.RENAISSANCE.features.payment.controller;

import com.PMRGSolution.RENAISSANCE.core.security.CustomUserDetails;
import com.PMRGSolution.RENAISSANCE.features.payment.dto.*;
import com.PMRGSolution.RENAISSANCE.features.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.createOrder(request, user.getId()));
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<String> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        paymentService.verifyPayment(request);
        return ResponseEntity.ok("Payment verified and subscription activated.");
    }

    @PostMapping("/validate-coupon")
    public ResponseEntity<CouponValidationResponse> validate(@RequestParam String code, @RequestParam Double currentPrice) {
        return ResponseEntity.ok(paymentService.validateCoupon(code, null, currentPrice));
    }

    // Admin
    @GetMapping("/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(paymentService.getAllSubscriptions());
    }

    @GetMapping("/admin/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransactionAdminResponse>> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }
}