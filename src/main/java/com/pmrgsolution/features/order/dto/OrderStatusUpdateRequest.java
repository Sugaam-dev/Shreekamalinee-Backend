package com.pmrgsolution.features.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status; // PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    private String paymentStatus;
    private String trackingNumber;
    private String courierPartner;
    private String trackingUrl;
    private String estimatedDeliveryDate;
    private String cancellationReason;
}