package com.pmrgsolution.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
public enum PaymentMethod {
    WHATSAPP("WhatsApp Order Booking"),
    RAZORPAY("Razorpay Online Payment"),
    COD("Cash on Delivery"),
    DIRECT_BANK("Direct Bank Transfer"),
    UPI("Direct UPI Transfer"),
    MANUAL("Manual Payment");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        // Support common incoming variations seamlessly
        if ("WHATSAPP_UPI".equals(normalized) || "UPI_DIRECT".equals(normalized) || "DIRECT_UPI".equals(normalized)) {
            return UPI;
        }
        if ("BANK_TRANSFER".equals(normalized)) {
            return DIRECT_BANK;
        }
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