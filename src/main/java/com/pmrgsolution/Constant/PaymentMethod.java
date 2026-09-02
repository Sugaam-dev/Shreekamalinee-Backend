package com.pmrgsolution.Constant;

public enum PaymentMethod {
    RAZORPAY("Razorpay Online Payment"),
    MANUAL("Manual UPI / Bank Transfer"),
    COD("Cash on Delivery"),
    WHATSAPP("WhatsApp Order Booking");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
