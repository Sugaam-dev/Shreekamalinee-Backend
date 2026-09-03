package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.order.dto.OrderEmailContext;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailUsageService emailUsageService;

    @Value("${app.mail.from:${spring.mail.username:onboarding@resend.dev}}")
    private String fromEmail;

    private static final String STORE_NAME = "Shreekamalinee";
    private static final String SUPPORT_URL = "https://www.shreekamalinee.com";

    // =========================================================================
    // 1. AUTH & SECURITY (no JPA risk — only strings)
    // =========================================================================

    @Async
    @Override
    public void sendOtpEmail(String to, String otp) {
        Context ctx = new Context();
        ctx.setVariable("otp", otp);
        dispatch(to, "Verify your Shreekamalinee Account", "email/customer/otp-verification", ctx);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String otp) {
        Context ctx = new Context();
        ctx.setVariable("otp", otp);
        dispatch(to, "Reset your Shreekamalinee Password", "email/customer/password-reset", ctx);
    }

    @Async
    @Override
    public void sendPasswordChangedAlert(String to, String userName) {
        Context ctx = new Context();
        ctx.setVariable("userName", safe(userName, "Valued Patron"));
        dispatch(to, "Security Alert: Your Password Was Changed", "email/customer/password-changed", ctx);
    }

    @Async
    @Override
    public void sendAccountLockedAlert(String to, String userName) {
        Context ctx = new Context();
        ctx.setVariable("userName", safe(userName, "Valued Patron"));
        dispatch(to, "Security Alert: Account Temporarily Locked", "email/customer/account-locked", ctx);
    }

    @Async
    @Override
    public void sendAccountStatusChangedAlert(String to, String userName, boolean enabled) {
        Context ctx = new Context();
        ctx.setVariable("userName", safe(userName, "Valued Patron"));
        ctx.setVariable("enabled", enabled);
        dispatch(to, "Shreekamalinee Account Status: " + (enabled ? "Reactivated" : "Suspended"),
                "email/customer/account-status", ctx);
    }

    // =========================================================================
    // 2. CUSTOMER ORDER LIFECYCLE
    //    All methods receive OrderEmailContext — zero JPA risk
    // =========================================================================

    @Async
    @Override
    public void sendOrderConfirmationEmail(String to, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFirstName());
        populatePricing(ctx, emailCtx);
        populateItems(ctx, emailCtx);
        populateAddress(ctx, emailCtx);
        dispatch(to, "Order Confirmed: " + emailCtx.getOrderNumber() + " — Shreekamalinee",
                "email/customer/order-confirmation", ctx);
    }

    @Async
    @Override
    public void sendPaymentProofReceivedEmail(String to, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFirstName());
        ctx.setVariable("utrNumber", safe(emailCtx.getUtrNumber(), "Submitted via Screenshot"));
        ctx.setVariable("orderTrackingUrl", emailCtx.getOrderTrackingUrl());
        populatePricing(ctx, emailCtx);
        dispatch(to, "Payment Proof Received: " + emailCtx.getOrderNumber() + " (Under Verification) — Shreekamalinee",
                "email/customer/payment-proof-received", ctx);
    }

    @Async
    @Override
    public void sendOrderShippedEmail(String to, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFirstName());
        ctx.setVariable("courierName", safe(emailCtx.getCourierPartner(), "Standard Courier"));
        ctx.setVariable("trackingNumber", emailCtx.getTrackingNumber());
        ctx.setVariable("trackingUrl", emailCtx.getTrackingUrl());
        ctx.setVariable("estimatedDeliveryDate", emailCtx.getEstimatedDeliveryDateDisplay());
        dispatch(to, "Your Saree is Dispatched! 🚚 Track Order " + emailCtx.getOrderNumber(),
                "email/customer/order-shipped", ctx);
    }

    @Async
    @Override
    public void sendOrderDeliveredEmail(String to, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFirstName());
        ctx.setVariable("reviewUrl", SUPPORT_URL + "/account/orders");
        dispatch(to, "Delivered: Your Luxury Handloom Saree Has Arrived! ✨ — " + emailCtx.getOrderNumber(),
                "email/customer/order-delivered", ctx);
    }

    @Async
    @Override
    public void sendOrderCancelledEmail(String to, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFirstName());
        ctx.setVariable("cancelReason", safe(emailCtx.getCancellationReason(),
                "Customer request / Administrative cancellation"));
        ctx.setVariable("refundAmountFormatted", emailCtx.getFinalTotalFormatted());
        ctx.setVariable("refundApplicable", emailCtx.isRefundApplicable());
        dispatch(to, "Order Cancelled: " + emailCtx.getOrderNumber() + " — Shreekamalinee",
                "email/customer/order-cancelled", ctx);
    }

    // =========================================================================
    // 3. ADMIN ALERTS
    // =========================================================================

    @Async
    @Override
    public void sendAdminNewOrderAlert(String adminEmail, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFullName());
        ctx.setVariable("customerEmail", emailCtx.getCustomerEmail());
        ctx.setVariable("customerCityState", emailCtx.getCustomerCityState());
        ctx.setVariable("paymentMethod", emailCtx.getPaymentMethod());
        ctx.setVariable("paymentStatus", emailCtx.getPaymentStatus());
        ctx.setVariable("adminOrderUrl", emailCtx.getAdminOrderUrl());
        populatePricing(ctx, emailCtx);
        populateItems(ctx, emailCtx);
        dispatch(adminEmail,
                "🚨 New Order " + emailCtx.getOrderNumber() + " - " +
                        emailCtx.getFinalTotalFormatted() + " (" + emailCtx.getCustomerFullName() + ")",
                "email/admin/new-order-alert", ctx);
    }

    @Async
    @Override
    public void sendAdminVipOrderAlert(String adminEmail, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFullName());
        ctx.setVariable("adminOrderUrl", emailCtx.getAdminOrderUrl());
        populatePricing(ctx, emailCtx);
        dispatch(adminEmail,
                "💎 VIP Order Alert: " + emailCtx.getOrderNumber() +
                        " (" + emailCtx.getFinalTotalFormatted() + ") - " + emailCtx.getCustomerFullName(),
                "email/admin/vip-order-alert", ctx);
    }

    @Async
    @Override
    public void sendAdminManualPaymentUploadedAlert(String adminEmail, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFullName());
        ctx.setVariable("utrNumber", safe(emailCtx.getUtrNumber(), "Not provided"));
        ctx.setVariable("adminOrderUrl", SUPPORT_URL + "/admin/orders");
        populatePricing(ctx, emailCtx);
        dispatch(adminEmail,
                "⚠️ Action Needed: Verify UPI Payment Receipt (" + emailCtx.getOrderNumber() + ")",
                "email/admin/manual-payment-alert", ctx);
    }

    @Async
    @Override
    public void sendAdminOrderCancelledAlert(String adminEmail, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFullName());
        ctx.setVariable("adminOrderUrl", SUPPORT_URL + "/admin/orders");
        populatePricing(ctx, emailCtx);
        dispatch(adminEmail,
                "🛑 Customer Cancelled Order " + emailCtx.getOrderNumber(),
                "email/admin/order-cancelled-alert", ctx);
    }

    @Async
    @Override
    public void sendAdminPaymentApprovedAlert(String adminEmail, OrderEmailContext emailCtx) {
        Context ctx = new Context();
        ctx.setVariable("orderNumber", emailCtx.getOrderNumber());
        ctx.setVariable("customerName", emailCtx.getCustomerFullName());
        ctx.setVariable("paymentMethod", emailCtx.getPaymentMethodDisplay());
        ctx.setVariable("adminOrderUrl", emailCtx.getAdminOrderUrl());
        populatePricing(ctx, emailCtx);
        dispatch(adminEmail,
                "✅ Payment Approved: " + emailCtx.getOrderNumber() +
                        " (" + emailCtx.getFinalTotalFormatted() + ") — " + emailCtx.getPaymentMethodDisplay(),
                "email/admin/payment-approved-alert", ctx);
    }

    @Async
    @Override
    public void sendAdminNewInquiryAlert(String adminEmail, String name, String email,
                                          String phone, String subject, String message) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("email", email);
        ctx.setVariable("phone", phone);
        ctx.setVariable("subject", subject);
        ctx.setVariable("message", message);
        ctx.setVariable("inquiriesUrl", SUPPORT_URL + "/admin/inquiries");
        dispatch(adminEmail,
                "📩 New Customer Inquiry: " + subject + " (" + name + ")",
                "email/admin/new-inquiry-alert", ctx);
    }

    // =========================================================================
    // 4. PRIVATE HELPERS
    // =========================================================================

    private void populatePricing(Context ctx, OrderEmailContext e) {
        ctx.setVariable("subtotal", e.getSubtotal());
        ctx.setVariable("subtotalFormatted", e.getSubtotalFormatted());
        ctx.setVariable("discount", e.getDiscountAmount());
        ctx.setVariable("discountFormatted", e.getDiscountFormatted());
        ctx.setVariable("couponCode", e.getCouponCode());
        ctx.setVariable("shippingFee", e.getShippingFee());
        ctx.setVariable("shippingFormatted", e.getShippingFormatted());
        ctx.setVariable("codFee", e.getCodHandlingFee());
        ctx.setVariable("codFeeFormatted", e.getCodFeeFormatted());
        ctx.setVariable("finalTotal", e.getFinalAmount());
        ctx.setVariable("finalTotalFormatted", e.getFinalTotalFormatted());
        ctx.setVariable("formulaText", e.getFormulaText());
        ctx.setVariable("paymentMethodDisplay", e.getPaymentMethodDisplay());
        ctx.setVariable("paymentStatus", e.getPaymentStatus());
        ctx.setVariable("paymentStatusColor", e.getPaymentStatusColor());
    }

    private void populateItems(Context ctx, OrderEmailContext e) {
        ctx.setVariable("orderItems", e.getOrderItems());
    }

    private void populateAddress(Context ctx, OrderEmailContext e) {
        ctx.setVariable("hasShippingAddress", e.isHasShippingAddress());
        ctx.setVariable("recipientName", e.getRecipientName());
        ctx.setVariable("addressLine1", e.getAddressLine1());
        ctx.setVariable("addressLine2", e.getAddressLine2());
        ctx.setVariable("city", e.getCity());
        ctx.setVariable("state", e.getState());
        ctx.setVariable("postalCode", e.getPostalCode());
        ctx.setVariable("country", e.getCountry());
        ctx.setVariable("phoneNumber", e.getPhoneNumber());
    }

    private void dispatch(String to, String subject, String templatePath, Context ctx) {
        if (to == null || to.isBlank() || !to.contains("@")) {
            log.warn("Skipping email: invalid recipient '{}'", to);
            return;
        }
        try {
            ctx.setVariable("emailSubject", subject);
            ctx.setVariable("supportUrl", SUPPORT_URL);
            String html = templateEngine.process(templatePath, ctx);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, STORE_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            // 🔄 Smart Resend Rate-Limit & Network Resilience (Auto-retry up to 3 times)
            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    mailSender.send(msg);
                    emailUsageService.recordEmailSent();
                    log.info("Email dispatched successfully: '{}' → {}", subject, maskEmail(to));
                    break; // Succeeded!
                } catch (Exception sendEx) {
                    if (attempt < maxRetries) {
                        // PERF FIX: Removed Thread.sleep — blocking an async thread pool thread
                        // for 600-1800ms under high load starves other async tasks.
                        // Fast-retry is acceptable since email dispatch is already async & non-blocking.
                        log.warn("Email send attempt {} failed for {}. Retrying immediately... Error: {}",
                                attempt, maskEmail(to), sendEx.getMessage());
                    } else {
                        log.error("Failed to dispatch email '{}' → {} after {} attempts: {}",
                                subject, maskEmail(to), maxRetries, sendEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to prepare email '{}' → {}: {}", subject, maskEmail(to), e.getMessage());
        }
    }

    private String safe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "******";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name.charAt(0) + "***@" + parts[1];
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}