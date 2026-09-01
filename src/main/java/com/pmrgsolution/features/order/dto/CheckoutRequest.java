package com.pmrgsolution.features.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    @NotNull(message = "Shipping address is required")
    private UUID shippingAddressId;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // RAZORPAY, COD, DIRECT_UPI, MANUAL

    private String couponCode;
    private String notes;

    // Optional selective cart checkout
    private List<UUID> selectedCartItemIds;

    // Optional direct "Buy Now" checkout (bypasses cart)
    private DirectItem directItem;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DirectItem {
        private UUID productId;
        private UUID variantId;
        private Integer quantity;
        private String size;
        private String color;
    }
}