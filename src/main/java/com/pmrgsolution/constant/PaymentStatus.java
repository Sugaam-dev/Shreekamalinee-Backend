package com.pmrgsolution.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum PaymentStatus {
    PENDING("Payment Pending"),
    PENDING_VERIFICATION("Pending Receipt Verification"),
    PAID("Payment Confirmed"),
    SUCCESS("Payment Successful"),
    FAILED("Payment Failed"),
    REFUND_PENDING("Refund Processing"),
    REFUNDED("Payment Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static PaymentStatus fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(s -> s.name().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}