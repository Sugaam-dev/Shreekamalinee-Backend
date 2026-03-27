package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionAdminResponse {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String userEmail;
    private String packageName; // Changed from categoryName
    private Double amount;
    private String couponCode;
    private String status;
    private LocalDateTime createdAt;
}