package com.pmrgsolution.features.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminManualOrderRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    private String customerEmail;

    @NotBlank(message = "Address is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal Code is required")
    private String postalCode;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    private UUID variantId;

    @Builder.Default
    private int quantity = 1;

    private String couponCode;

    @Builder.Default
    private String paymentMethod = "WHATSAPP_UPI"; // WHATSAPP_UPI, DIRECT_BANK, COD

    @Builder.Default
    private String paymentStatus = "PAID"; // PAID, PENDING

    private String notes;

    @Builder.Default
    private Boolean sendEmailNotification = true;
}
