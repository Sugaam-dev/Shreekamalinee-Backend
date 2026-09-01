package com.pmrgsolution.features.payment.controller;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.payment.dto.PaymentVerificationRequest;
import com.pmrgsolution.features.payment.dto.TransactionResponse;
import com.pmrgsolution.features.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/payments", "/api/v1/orders/razorpay"})
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}/create")
    public ResponseEntity<OrderResponse> createPaymentOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(orderId, user.getId()));
    }

    /**
     * SECURITY FIX: userId is now passed to the service to validate the caller owns the order.
     * Previously, any authenticated user could mark any order as PAID using their own Razorpay signature.
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        paymentService.verifyRazorpayPayment(request, user.getId());
        return ResponseEntity.ok("Payment verified successfully.");
    }

    @PostMapping(value = "/{orderId}/manual", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionResponse> submitManualPayment(
            @PathVariable UUID orderId,
            @RequestParam("utrNumber") String utrNumber,
            @RequestParam(value = "receipt", required = false) org.springframework.web.multipart.MultipartFile receipt,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.submitManualPayment(orderId, utrNumber, receipt, user.getId()));
    }

    /**
     * SECURITY FIX: userId is now validated so a user can only view their own transaction.
     */
    @GetMapping("/transaction/{orderId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.getTransactionByOrderId(orderId, user.getId()));
    }
}