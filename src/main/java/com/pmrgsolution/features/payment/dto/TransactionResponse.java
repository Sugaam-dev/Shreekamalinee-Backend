package com.pmrgsolution.features.payment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private UUID id;
    private UUID orderId;
    private String transactionId;
    private String gateway;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;
}