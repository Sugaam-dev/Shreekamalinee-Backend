package com.pmrgsolution.features.order.dto;

import com.pmrgsolution.constant.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String cancellationReason;
}