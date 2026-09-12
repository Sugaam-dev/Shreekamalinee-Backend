package com.pmrgsolution.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum OrderStatus {
    PENDING("Order Pending"),
    PLACED("Order Placed"),
    PAYMENT_PROOF_SUBMITTED("Payment Proof Submitted"),
    CONFIRMED("Order Confirmed"),
    PROCESSING("Quality Check & Processing"),
    SHIPPED("Dispatched with Courier"),
    DELIVERED("Delivered to Patron"),
    CANCELLED("Order Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case PENDING -> target == PLACED || target == PAYMENT_PROOF_SUBMITTED || target == CONFIRMED || target == PROCESSING || target == CANCELLED;
            case PLACED -> target == PAYMENT_PROOF_SUBMITTED || target == CONFIRMED || target == PROCESSING || target == CANCELLED;
            case PAYMENT_PROOF_SUBMITTED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PROCESSING || target == SHIPPED || target == CANCELLED;
            case PROCESSING -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED || target == CANCELLED;
            case DELIVERED -> false;
            case CANCELLED -> false;
        };
    }

    @JsonCreator
    public static OrderStatus fromString(String value) {
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