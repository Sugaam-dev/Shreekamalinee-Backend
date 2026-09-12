package com.pmrgsolution.features.payment.dto;

import com.pmrgsolution.constant.PaymentGateway;
import com.pmrgsolution.constant.PaymentMethod;
import com.pmrgsolution.constant.PaymentStatus;
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
    private PaymentGateway gateway;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
}