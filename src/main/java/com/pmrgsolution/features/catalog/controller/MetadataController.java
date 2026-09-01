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
