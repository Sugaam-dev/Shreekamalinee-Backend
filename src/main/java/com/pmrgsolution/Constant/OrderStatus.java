package com.pmrgsolution.Constant;

public enum OrderStatus {
    PLACED("Order Placed"),
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
            case PLACED -> target == PROCESSING || target == CANCELLED;
            case PROCESSING -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED || target == CANCELLED;
            case DELIVERED -> false;
            case CANCELLED -> false;
        };
    }
}
