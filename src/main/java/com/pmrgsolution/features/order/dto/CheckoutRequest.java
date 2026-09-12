package com.pmrgsolution.features.order.dto;

import com.pmrgsolution.constant.PaymentMethod;
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
    private PaymentMethod paymentMethod;

    private String couponCode;
    private String notes;

    private List<UUID> selectedCartItemIds;
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