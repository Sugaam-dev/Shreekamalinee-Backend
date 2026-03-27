package com.PMRGSolution.RENAISSANCE.features.payment.service;

import com.PMRGSolution.RENAISSANCE.features.payment.dto.*;
import java.util.List;
import java.util.UUID;

public interface PaymentService {
    // Core Transaction Flow
    OrderResponse createOrder(OrderRequest request, UUID userId);
    void verifyPayment(PaymentVerificationRequest request);

    // Admin Reports
    List<UserSubscriptionResponse> getAllSubscriptions();
    List<TransactionAdminResponse> getAllTransactions();
    
    // Coupons
    CouponValidationResponse validateCoupon(String code, UUID categoryId, Double currentPrice);
    void createCoupon(CouponRequest request);
    List<CouponResponse> getAllCoupons();
    void deleteCoupon(UUID id);
}