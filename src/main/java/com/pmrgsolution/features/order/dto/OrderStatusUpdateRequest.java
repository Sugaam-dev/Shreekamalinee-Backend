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
    private String status; // CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    // Only used when status = CANCELLED
    private String cancellationReason;
}