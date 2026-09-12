package com.pmrgsolution.features.order.dto;

import com.pmrgsolution.constant.PaymentMethod;
import com.pmrgsolution.constant.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[0-9+() -]{10,15}$", message = "Please enter a valid phone number")
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
    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be a 6-digit PIN code")
    private String postalCode;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    private UUID variantId;

    @Builder.Default
    private int quantity = 1;

    private String couponCode;

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.UPI;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    private String notes;

    @Builder.Default
    private Boolean sendEmailNotification = true;
}