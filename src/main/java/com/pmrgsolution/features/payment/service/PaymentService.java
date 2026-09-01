package com.pmrgsolution.features.payment.service;

import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.payment.dto.PaymentVerificationRequest;
import com.pmrgsolution.features.payment.dto.TransactionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PaymentService {
    OrderResponse createRazorpayOrder(UUID orderId, UUID userId);
    /** SECURITY: userId required to validate the caller owns the order being paid */
    void verifyRazorpayPayment(PaymentVerificationRequest request, UUID userId);
    TransactionResponse submitManualPayment(UUID orderId, String utrNumber, MultipartFile receipt, UUID userId);
    /** SECURITY: userId required to validate the caller owns the order */
    TransactionResponse getTransactionByOrderId(UUID orderId, UUID userId);
}