package com.pmrgsolution.Constant;

public enum PaymentStatus {
    PENDING("Payment Pending"),
    PENDING_VERIFICATION("Pending Receipt Verification"),
    PAID("Payment Confirmed"),
    SUCCESS("Payment Successful"),
    FAILED("Payment Failed"),
    REFUNDED("Payment Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
