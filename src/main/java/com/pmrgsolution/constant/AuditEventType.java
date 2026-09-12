package com.pmrgsolution.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    PASSWORD_RESET,
    ORDER_PLACED,
    ADMIN_ACTION,
    STATUS_CHANGE;

    @JsonCreator
    public static AuditEventType fromString(String value) {
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