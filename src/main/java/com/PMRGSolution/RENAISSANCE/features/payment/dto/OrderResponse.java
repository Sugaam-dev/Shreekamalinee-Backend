package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private String razorpayOrderId;
    private String currency;
    private Integer amount; // Amount in PAISE (Razorpay requirement)
    private String keyId;   // Your Razorpay Public Key
    private String businessName; // e.g., "Renaissance Exam Engine"
}