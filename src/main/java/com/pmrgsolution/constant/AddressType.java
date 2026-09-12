package com.pmrgsolution.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum AddressType {
    HOME("Home Address"),
    WORK("Work Address"),
    OTHER("Other Address"),
    MANUAL_ORDER("Manual Order Address");

    private final String displayName;

    AddressType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static AddressType fromString(String value) {
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