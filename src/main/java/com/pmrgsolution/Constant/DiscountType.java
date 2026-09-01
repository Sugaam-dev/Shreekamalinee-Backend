package com.pmrgsolution.Constant;

public enum DiscountType {
    PERCENTAGE("Percentage Discount (%)"),
    FIXED("Fixed Amount Discount (₹)");

    private final String displayName;

    DiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
