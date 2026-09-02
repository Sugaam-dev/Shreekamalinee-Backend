package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.order.dto.OrderEmailContext;

/**
 * Transactional email service interface.
 *
 * IMPORTANT: All order-related methods accept {@link OrderEmailContext} — a plain
 * immutable DTO. NEVER pass JPA entities directly to these methods, because they
 * are dispatched via @Async and run in a separate thread pool with no Hibernate Session.
 */
public interface EmailService {

    // =========================================================================
    // 1. AUTHENTICATION & SECURITY
    // =========================================================================
    void sendOtpEmail(String to, String otp);
    void sendPasswordResetEmail(String to, String otp);
    void sendPasswordChangedAlert(String to, String userName);
    void sendAccountLockedAlert(String to, String userName);
    void sendAccountStatusChangedAlert(String to, String userName, boolean enabled);

    // =========================================================================
    // 2. CUSTOMER ORDER LIFECYCLE
    //    All methods receive OrderEmailContext — fully resolved plain Java object.
    // =========================================================================

    /** Order confirmed (COD, Razorpay payment success, or admin payment approval). */
    void sendOrderConfirmationEmail(String to, OrderEmailContext ctx);

    /** Customer submitted UPI/bank transfer proof — under verification. */
    void sendPaymentProofReceivedEmail(String to, OrderEmailContext ctx);

    /** Order dispatched — courier tracking details sent. */
    void sendOrderShippedEmail(String to, OrderEmailContext ctx);

    /** Order delivered — invite review. */
    void sendOrderDeliveredEmail(String to, OrderEmailContext ctx);

    /** Order cancelled — with refund info if applicable. */
    void sendOrderCancelledEmail(String to, OrderEmailContext ctx);

    // =========================================================================
    // 3. ADMIN STORE ALERTS
    // =========================================================================

    /** New order received by the store. */
    void sendAdminNewOrderAlert(String adminEmail, OrderEmailContext ctx);

    /** High-value VIP order alert. */
    void sendAdminVipOrderAlert(String adminEmail, OrderEmailContext ctx);

    /** Customer uploaded UPI payment proof — admin action required. */
    void sendAdminManualPaymentUploadedAlert(String adminEmail, OrderEmailContext ctx);

    /** Customer or admin cancelled an order. */
    void sendAdminOrderCancelledAlert(String adminEmail, OrderEmailContext ctx);

    /** Admin approved a payment (UPI/COD/Manual) — admin dashboard confirmation. */
    void sendAdminPaymentApprovedAlert(String adminEmail, OrderEmailContext ctx);

    /** New contact form / WhatsApp inquiry submitted. */
    void sendAdminNewInquiryAlert(String adminEmail, String name, String email,
                                   String phone, String subject, String message);
}