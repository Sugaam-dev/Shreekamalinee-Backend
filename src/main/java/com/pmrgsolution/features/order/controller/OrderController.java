package com.pmrgsolution.features.order.controller;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.order.dto.CheckoutRequest;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.order.service.OrderService;
import com.pmrgsolution.features.settings.dto.BankDetailsResponse;
import com.pmrgsolution.features.settings.dto.StoreSettingsResponse;
import com.pmrgsolution.features.settings.service.StoreSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.pmrgsolution.features.payment.dto.TransactionResponse;
import com.pmrgsolution.features.payment.service.PaymentService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final StoreSettingsService storeSettingsService;

    /**
     * SECURITY FIX: Returns only banking payment details — NOT the full store settings.
     * Previously, the full StoreSettingsResponse leaked internal fees, promo flags, and config.
     */
    @GetMapping("/bank-details")
    public ResponseEntity<BankDetailsResponse> getBankDetails() {
        StoreSettingsResponse s = storeSettingsService.getStoreSettings();
        return ResponseEntity.ok(BankDetailsResponse.builder()
                .accountHolderName(s.getAccountHolderName())
                .accountNumber(s.getAccountNumber())
                .ifscCode(s.getIfscCode())
                .bankName(s.getBankName())
                .branchName(s.getBranchName())
                .upiId(s.getUpiId())
                .qrCodeUrl(s.getQrCodeUrl())
                .whatsappNumber(s.getWhatsappNumber())
                .build());
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(user.getId(), request, idempotencyKey));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getUserOrders(user.getId(), pageable));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(orderService.getOrderById(user.getId(), orderId));
    }

    /**
     * SECURITY FIX: Requires authentication. Validates that the user owns the order.
     * Previously, this endpoint was public and returned full customer PII (name, address, phone).
     */
    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<OrderResponse> trackOrderByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(orderService.getOrderByOrderNumberForUser(orderNumber, user.getId()));
    }

    @PostMapping(value = "/{orderId}/manual", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionResponse> submitManualPayment(
            @PathVariable UUID orderId,
            @RequestParam("utrNumber") String utrNumber,
            @RequestParam(value = "receipt", required = false) MultipartFile receipt,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.submitManualPayment(orderId, utrNumber, receipt, user.getId()));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrderCustomer(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(orderService.cancelOrderCustomer(user.getId(), orderId));
    }
}