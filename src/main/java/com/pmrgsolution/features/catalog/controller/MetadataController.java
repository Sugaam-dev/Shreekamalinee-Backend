package com.pmrgsolution.features.catalog.controller;

import com.pmrgsolution.constant.*;
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
                Map.of("id", "UPI", "name", "Direct QR Code / UPI Transfer", "isImplemented", true, "badge", "Zero Gateway Surcharge"),
                Map.of("id", "COD", "name", "Cash on Delivery (COD)", "isImplemented", true, "badge", "Doorstep Payment"),
                Map.of("id", "WHATSAPP", "name", "WhatsApp Assisted Booking & Payment", "isImplemented", true, "badge", "Personal Concierge"),
                Map.of("id", "DIRECT_BANK", "name", "Direct Bank Transfer", "isImplemented", true, "badge", "NEFT / RTGS"),
                Map.of("id", "RAZORPAY", "name", "Razorpay Secure Online Checkout", "isImplemented", false, "badge", "Unavailable", "disabledReason", "Online Payment Gateway is not implemented")
        );
        metadata.put("paymentGateways", paymentGateways);

        // Admin / Manual Order Payment Methods
        List<Map<String, Object>> adminPaymentMethods = List.of(
                Map.of("key", "UPI", "label", "Direct UPI / QR Transfer", "isSelectable", true),
                Map.of("key", "DIRECT_BANK", "label", "Direct Bank Transfer (NEFT/RTGS)", "isSelectable", true),
                Map.of("key", "WHATSAPP", "label", "WhatsApp Assisted Order", "isSelectable", true),
                Map.of("key", "COD", "label", "Cash on Delivery (COD)", "isSelectable", true),
                Map.of("key", "MANUAL", "label", "Manual Payment", "isSelectable", true),
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

        // Address Types with display names
        List<Map<String, String>> addressTypes = Arrays.stream(AddressType.values())
                .map(s -> Map.of("key", s.name(), "label", s.getDisplayName()))
                .toList();
        metadata.put("addressTypes", addressTypes);

        // Contact Message Statuses with display names
        List<Map<String, String>> contactMessageStatuses = Arrays.stream(ContactMessageStatus.values())
                .map(s -> Map.of("key", s.name(), "label", s.getDisplayName()))
                .toList();
        metadata.put("contactMessageStatuses", contactMessageStatuses);

        return ResponseEntity.ok(metadata);
    }
}
