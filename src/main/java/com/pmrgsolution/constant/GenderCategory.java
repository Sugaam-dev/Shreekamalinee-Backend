package com.pmrgsolution.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum GenderCategory {
    WOMEN("Women's Collection"),
    MEN("Men's Collection"),
    KIDS("Kids' Collection"),
    UNISEX("Unisex Heritage");

    private final String displayName;

    GenderCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static GenderCategory fromString(String value) {
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