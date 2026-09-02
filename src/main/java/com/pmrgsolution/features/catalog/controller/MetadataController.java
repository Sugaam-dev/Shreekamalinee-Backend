package com.pmrgsolution.features.catalog.controller;

import com.pmrgsolution.Constant.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/metadata")
public class MetadataController {

    @GetMapping("/enums")
    public ResponseEntity<Map<String, Object>> getSystemEnums() {
        Map<String, Object> metadata = new LinkedHashMap<>();

        // Order Statuses with display names
        List<Map<String, String>> orderStatuses = Arrays.stream(OrderStatus.values())
                .map(s -> Map.of("key", s.name(), "label", s.getDisplayName()))
                .toList();
        metadata.put("orderStatuses", orderStatuses);

        // Payment Statuses with display names
        List<Map<String, String>> paymentStatuses = Arrays.stream(PaymentStatus.values())
                .map(s -> Map.of("key", s.name(), "label", s.getDisplayName()))
                .toList();
        metadata.put("paymentStatuses", paymentStatuses);

        // Payment Methods with display names
        List<Map<String, String>> paymentMethods = Arrays.stream(PaymentMethod.values())
                .map(s -> Map.of("key", s.name(), "label", s.getDisplayName()))
                .toList();
        metadata.put("paymentMethods", paymentMethods);

        // Payment Gateways for customer storefront
        List<Map<String, Object>> paymentGateways = List.of(
                Map.of("id", "UPI_DIRECT", "name", "Direct QR Code / UPI UTR Confirmation", "isImplemented", true, "badge", "Zero Gateway Surcharge"),
                Map.of("id", "COD", "name", "Cash on Delivery (COD)", "isImplemented", true, "badge", "Doorstep Payment"),
                Map.of("id", "WHATSAPP", "name", "WhatsApp Assisted Booking & Payment", "isImplemented", true, "badge", "Personal Concierge"),
                Map.of("id", "RAZORPAY", "name", "Razorpay Secure Online Checkout", "isImplemented", false, "badge", "Unavailable", "disabledReason", "Online Payment Gateway is not implemented")
        );
        metadata.put("paymentGateways", paymentGateways);

        // Admin / Manual Order Payment Methods (Admin can see Razorpay marked as non-selectable)
        List<Map<String, Object>> adminPaymentMethods = List.of(
                Map.of("key", "WHATSAPP_UPI", "label", "WhatsApp Direct UPI / GPay / PhonePe", "isSelectable", true),
                Map.of("key", "DIRECT_BANK", "label", "NEFT / RTGS Direct Bank Transfer", "isSelectable", true),
                Map.of("key", "COD", "label", "Cash on Delivery (COD)", "isSelectable", true),
                Map.of("key", "MANUAL", "label", "Manual Offline Payment", "isSelectable", true),
                Map.of("key", "RAZORPAY", "label", "Razorpay Online Gateway (Not Implemented)", "isSelectable", false)
        );
        metadata.put("adminPaymentMethods", adminPaymentMethods);

        // Discount Types with display names
        List<Map<String, String>> discountTypes = Arrays.stream(DiscountType.values())
                .map(s -> Map.of("key", s.name(), "label", s.getDisplayName()))
                .toList();
        metadata.put("discountTypes", discountTypes);

        // Gender Categories with display names
        List<Map<String, String>> genderCategories = Arrays.stream(GenderCategory.values())
                .map(s -> Map.of("key", s.name(), "label", s.getDisplayName()))
                .toList();
        metadata.put("genderCategories", genderCategories);

        return ResponseEntity.ok(metadata);
    }
}
