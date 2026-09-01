package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.order.entity.Order;

public interface EmailService {
    // --- Auth & Security ---
    void sendOtpEmail(String to, String otp);
    void sendPasswordResetEmail(String to, String otp);
    void sendPasswordChangedAlert(String to, String userName);
    void sendAccountLockedAlert(String to, String userName);
    void sendAccountStatusChangedAlert(String to, String userName, boolean enabled);

    // --- Customer Order Lifecycle ---
    void sendOrderConfirmationEmail(String to, Order order);
    void sendOrderConfirmationEmail(String to, String orderNumber, String totalAmount);
    void sendPaymentProofReceivedEmail(String to, Order order, String utrNumber);
    void sendOrderShippedEmail(String to, Order order);
    void sendOrderDeliveredEmail(String to, Order order);
    void sendOrderCancelledEmail(String to, Order order, String reason);

    // --- Store Admin Alerts ---
    void sendAdminNewOrderAlert(String adminEmail, Order order);
    void sendAdminVipOrderAlert(String adminEmail, Order order);
    void sendAdminManualPaymentUploadedAlert(String adminEmail, Order order);
    void sendAdminOrderCancelledAlert(String adminEmail, Order order);
    void sendAdminNewInquiryAlert(String adminEmail, String name, String email, String phone, String subject, String message);
}