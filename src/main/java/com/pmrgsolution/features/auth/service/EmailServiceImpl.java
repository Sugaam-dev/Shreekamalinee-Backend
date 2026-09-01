package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.address.entity.ShippingAddress;
import com.pmrgsolution.features.order.entity.Order;
import com.pmrgsolution.features.order.entity.OrderItem;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:${spring.mail.username:onboarding@resend.dev}}")
    private String fromEmail;

    private static final String STORE_NAME = "Shreekamalinee";
    private static final String SUPPORT_URL = "https://www.shreekamalinee.com";
    private static final NumberFormat INR_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    // =========================================================================
    // 1. AUTHENTICATION & SECURITY NOTIFICATIONS
    // =========================================================================

    @Async
    @Override
    public void sendOtpEmail(String to, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        sendTemplateEmail(to, "Verify your Shreekamalinee Account", "email/customer/otp-verification", context);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        sendTemplateEmail(to, "Reset your Shreekamalinee Password", "email/customer/password-reset", context);
    }

    @Async
    @Override
    public void sendPasswordChangedAlert(String to, String userName) {
        Context context = new Context();
        context.setVariable("userName", (userName != null && !userName.isBlank()) ? userName : "Valued Patron");
        sendTemplateEmail(to, "Security Alert: Your Password Was Changed", "email/customer/password-changed", context);
    }

    @Async
    @Override
    public void sendAccountLockedAlert(String to, String userName) {
        Context context = new Context();
        context.setVariable("userName", (userName != null && !userName.isBlank()) ? userName : "Valued Patron");
        sendTemplateEmail(to, "Security Alert: Account Temporarily Locked", "email/customer/account-locked", context);
    }

    @Async
    @Override
    public void sendAccountStatusChangedAlert(String to, String userName, boolean enabled) {
        Context context = new Context();
        context.setVariable("userName", (userName != null && !userName.isBlank()) ? userName : "Valued Patron");
        context.setVariable("enabled", enabled);
        String statusText = enabled ? "Reactivated" : "Suspended";
        sendTemplateEmail(to, "Shreekamalinee Account Status: " + statusText, "email/customer/account-status", context);
    }

    // =========================================================================
    // 2. CUSTOMER ORDER LIFECYCLE NOTIFICATIONS
    // =========================================================================

    @Async
    @Override
    public void sendOrderConfirmationEmail(String to, Order order) {
        String orderNumber = (order.getOrderNumber() != null && !order.getOrderNumber().isBlank())
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? order.getUser().getFirstName() : "Valued Patron";

        Context context = new Context();
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("customerName", customerName);
        populateOrderItemsContext(context, order);
        populatePriceBreakdownContext(context, order);
        populateAddressContext(context, order.getShippingAddress());

        sendTemplateEmail(to, "Order Confirmed: " + orderNumber + " — Shreekamalinee", "email/customer/order-confirmation", context);
    }

    @Async
    @Override
    public void sendOrderConfirmationEmail(String to, String orderNumber, String totalAmount) {
        Context context = new Context();
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("customerName", "Valued Patron");
        context.setVariable("subtotalFormatted", "₹" + totalAmount);
        context.setVariable("finalTotalFormatted", "₹" + totalAmount);
        context.setVariable("formulaText", "Total = ₹" + totalAmount);
        context.setVariable("paymentMethodDisplay", "Direct Payment");
        context.setVariable("paymentStatus", "CONFIRMED");
        context.setVariable("paymentStatusColor", "#27ae60");
        context.setVariable("hasShippingAddress", false);
        context.setVariable("orderItems", List.of());

        sendTemplateEmail(to, "Order Confirmed: " + orderNumber + " — Shreekamalinee", "email/customer/order-confirmation", context);
    }

    @Async
    @Override
    public void sendPaymentProofReceivedEmail(String to, Order order, String utrNumber) {
        String orderNumber = order.getOrderNumber() != null ? order.getOrderNumber() : "SK-" + order.getId().toString().substring(0, 8).toUpperCase();
        String customerName = order.getUser() != null ? order.getUser().getFirstName() : "Valued Patron";
        String utrDisplay = (utrNumber != null && !utrNumber.isBlank()) ? utrNumber : "Submitted via Screenshot";

        Context context = new Context();
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("customerName", customerName);
        context.setVariable("utrNumber", utrDisplay);
        context.setVariable("orderTrackingUrl", SUPPORT_URL + "/account/orders");
        populatePriceBreakdownContext(context, order);

        sendTemplateEmail(to, "Payment Proof Received: " + orderNumber + " (Under Verification) — Shreekamalinee", "email/customer/payment-proof-received", context);
    }

    @Async
    @Override
    public void sendOrderShippedEmail(String to, Order order) {
        String orderNum = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? order.getUser().getFirstName() : "Valued Customer";
        String courier = order.getCourierName() != null ? order.getCourierName() : "BlueDart Express";
        String trackingNum = order.getTrackingNumber() != null ? order.getTrackingNumber() : "AWB" + order.getId().toString().substring(0, 8).toUpperCase();
        String trackingUrl = order.getTrackingUrl() != null && !order.getTrackingUrl().isBlank()
                ? order.getTrackingUrl() : (SUPPORT_URL + "/account/orders");
        String estDate = order.getEstimatedDeliveryDate() != null
                ? order.getEstimatedDeliveryDate().format(DateTimeFormatter.ofPattern("dd MMMM, yyyy")) : "3-5 Business Days";

        Context context = new Context();
        context.setVariable("orderNumber", orderNum);
        context.setVariable("customerName", customerName);
        context.setVariable("courierName", courier);
        context.setVariable("trackingNumber", trackingNum);
        context.setVariable("trackingUrl", trackingUrl);
        context.setVariable("estimatedDeliveryDate", estDate);

        sendTemplateEmail(to, "Your Saree is Dispatched! 🚚 Track Order " + orderNum, "email/customer/order-shipped", context);
    }

    @Async
    @Override
    public void sendOrderDeliveredEmail(String to, Order order) {
        String orderNum = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? order.getUser().getFirstName() : "Valued Customer";

        Context context = new Context();
        context.setVariable("orderNumber", orderNum);
        context.setVariable("customerName", customerName);
        context.setVariable("reviewUrl", SUPPORT_URL + "/account/orders");

        sendTemplateEmail(to, "Delivered: Your Luxury Handloom Saree Has Arrived! ✨ — " + orderNum, "email/customer/order-delivered", context);
    }

    @Async
    @Override
    public void sendOrderCancelledEmail(String to, Order order, String reason) {
        String orderNum = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? order.getUser().getFirstName() : "Valued Customer";
        BigDecimal total = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        boolean refundApplies = "PAID".equalsIgnoreCase(order.getPaymentStatus()) || !"COD".equalsIgnoreCase(order.getPaymentMethod());

        Context context = new Context();
        context.setVariable("orderNumber", orderNum);
        context.setVariable("customerName", customerName);
        context.setVariable("cancelReason", (reason != null && !reason.isBlank()) ? reason : "Customer request / Administrative cancellation");
        context.setVariable("refundAmountFormatted", formatCurrency(total));
        context.setVariable("refundApplicable", refundApplies);

        sendTemplateEmail(to, "Order Cancelled: " + orderNum + " — Shreekamalinee", "email/customer/order-cancelled", context);
    }

    // =========================================================================
    // 3. ADMIN MANAGEMENT ALERTS
    // =========================================================================

    @Async
    @Override
    public void sendAdminNewOrderAlert(String adminEmail, Order order) {
        String orderNum = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? (order.getUser().getFirstName() + " " + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "")) : "Patron Customer";
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : "N/A";
        String city = order.getShippingAddress() != null ? order.getShippingAddress().getCity() : "India";
        String state = order.getShippingAddress() != null ? order.getShippingAddress().getState() : "";
        BigDecimal total = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();

        Context context = new Context();
        context.setVariable("orderNumber", orderNum);
        context.setVariable("customerName", customerName);
        context.setVariable("customerEmail", customerEmail);
        context.setVariable("customerCityState", city + (state.isBlank() ? "" : ", " + state));
        context.setVariable("paymentMethod", order.getPaymentMethod());
        context.setVariable("paymentStatus", order.getPaymentStatus());
        context.setVariable("adminOrderUrl", SUPPORT_URL + "/admin/orders/" + order.getId());
        populateOrderItemsContext(context, order);
        populatePriceBreakdownContext(context, order);

        sendTemplateEmail(adminEmail, "🚨 New Order " + orderNum + " - " + formatCurrency(total) + " (" + customerName + ")", "email/admin/new-order-alert", context);
    }

    @Async
    @Override
    public void sendAdminVipOrderAlert(String adminEmail, Order order) {
        String orderNum = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? (order.getUser().getFirstName() + " " + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "")) : "Patron Customer";
        BigDecimal total = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();

        Context context = new Context();
        context.setVariable("orderNumber", orderNum);
        context.setVariable("customerName", customerName);
        context.setVariable("adminOrderUrl", SUPPORT_URL + "/admin/orders/" + order.getId());
        populatePriceBreakdownContext(context, order);

        sendTemplateEmail(adminEmail, "💎 VIP Order Alert: " + orderNum + " (" + formatCurrency(total) + ") - " + customerName, "email/admin/vip-order-alert", context);
    }

    @Async
    @Override
    public void sendAdminManualPaymentUploadedAlert(String adminEmail, Order order) {
        String orderNum = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? (order.getUser().getFirstName() + " " + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "")) : "Patron Customer";

        Context context = new Context();
        context.setVariable("orderNumber", orderNum);
        context.setVariable("customerName", customerName);
        context.setVariable("adminOrderUrl", SUPPORT_URL + "/admin/orders");
        populatePriceBreakdownContext(context, order);

        sendTemplateEmail(adminEmail, "⚠️ Action Needed: Verify UPI Payment Receipt (" + orderNum + ")", "email/admin/manual-payment-alert", context);
    }

    @Async
    @Override
    public void sendAdminOrderCancelledAlert(String adminEmail, Order order) {
        String orderNum = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber() : ("SK-" + order.getId().toString().substring(0, 8).toUpperCase());
        String customerName = order.getUser() != null ? (order.getUser().getFirstName() + " " + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "")) : "Customer";

        Context context = new Context();
        context.setVariable("orderNumber", orderNum);
        context.setVariable("customerName", customerName);
        context.setVariable("adminOrderUrl", SUPPORT_URL + "/admin/orders");
        populatePriceBreakdownContext(context, order);

        sendTemplateEmail(adminEmail, "🛑 Customer Cancelled Order " + orderNum, "email/admin/order-cancelled-alert", context);
    }

    @Async
    @Override
    public void sendAdminNewInquiryAlert(String adminEmail, String name, String email, String phone, String subject, String message) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("email", email);
        context.setVariable("phone", phone);
        context.setVariable("subject", subject);
        context.setVariable("message", message);
        context.setVariable("inquiriesUrl", SUPPORT_URL + "/admin/inquiries");

        sendTemplateEmail(adminEmail, "📩 New Customer Inquiry: " + subject + " (" + name + ")", "email/admin/new-inquiry-alert", context);
    }

    // =========================================================================
    // 4. PRIVATE HELPERS & TEMPLATE ENGINE DISPATCHER
    // =========================================================================

    private void sendTemplateEmail(String to, String subject, String templatePath, Context context) {
        if (to == null || to.isBlank() || !to.contains("@")) {
            log.warn("Skipping email dispatch: Invalid recipient address '{}'", to);
            return;
        }

        try {
            context.setVariable("emailSubject", subject);
            context.setVariable("supportUrl", SUPPORT_URL);

            String htmlBody = templateEngine.process(templatePath, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, STORE_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("Successfully dispatched transactional template email '{}' to {}", subject, maskEmail(to));
        } catch (Exception e) {
            log.error("Failed to send transactional template email '{}' to {}: {}", subject, maskEmail(to), e.getMessage());
        }
    }

    private void populateOrderItemsContext(Context ctx, Order order) {
        List<OrderItemView> items = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                String name = "Handloom Saree";
                String variant = "";
                if (item.getProductVariant() != null) {
                    if (item.getProductVariant().getProduct() != null) {
                        name = item.getProductVariant().getProduct().getName();
                    }
                    String size = item.getProductVariant().getSize() != null ? item.getProductVariant().getSize() : "";
                    String color = item.getProductVariant().getColor() != null ? item.getProductVariant().getColor() : "";
                    variant = (size + " " + color).trim();
                }
                BigDecimal unitPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                items.add(OrderItemView.builder()
                        .productName(name)
                        .variantInfo(variant)
                        .quantity(item.getQuantity())
                        .unitPrice(unitPrice)
                        .unitPriceFormatted(formatCurrency(unitPrice))
                        .totalPrice(totalPrice)
                        .totalPriceFormatted(formatCurrency(totalPrice))
                        .build());
            }
        }
        ctx.setVariable("orderItems", items);
    }

    private void populatePriceBreakdownContext(Context ctx, Order order) {
        BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal codFee = order.getCodHandlingFee() != null ? order.getCodHandlingFee() : BigDecimal.ZERO;
        BigDecimal finalTotal = order.getFinalAmount() != null ? order.getFinalAmount() : subtotal.subtract(discount).add(shipping).add(codFee);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        ctx.setVariable("subtotal", subtotal);
        ctx.setVariable("subtotalFormatted", formatCurrency(subtotal));
        ctx.setVariable("discount", discount);
        ctx.setVariable("discountFormatted", formatCurrency(discount));
        ctx.setVariable("couponCode", order.getCouponCode());
        ctx.setVariable("shippingFee", shipping);
        ctx.setVariable("shippingFormatted", formatCurrency(shipping));
        ctx.setVariable("codFee", codFee);
        ctx.setVariable("codFeeFormatted", formatCurrency(codFee));
        ctx.setVariable("finalTotal", finalTotal);
        ctx.setVariable("finalTotalFormatted", formatCurrency(finalTotal));

        // Explicit formula: e.g. Subtotal ₹2,000.00 - Discount ₹100.00 + Shipping ₹0.00 + COD ₹200.00 = ₹2,100.00
        StringBuilder formula = new StringBuilder("Subtotal ").append(formatCurrency(subtotal));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" - Discount ").append(formatCurrency(discount));
        }
        if (shipping.compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" + Shipping ").append(formatCurrency(shipping));
        } else {
            formula.append(" + Shipping ₹0.00 (FREE)");
        }
        if (codFee.compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" + COD Fee ").append(formatCurrency(codFee));
        }
        formula.append(" = ").append(formatCurrency(finalTotal));
        ctx.setVariable("formulaText", formula.toString());

        String methodDisplay = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? "Cash on Delivery (COD)"
                : "RAZORPAY".equalsIgnoreCase(order.getPaymentMethod()) ? "Online Payment (Razorpay)"
                : "WHATSAPP_UPI".equalsIgnoreCase(order.getPaymentMethod()) ? "WhatsApp Direct UPI"
                : "DIRECT_BANK".equalsIgnoreCase(order.getPaymentMethod()) ? "Direct Bank Transfer (NEFT/RTGS)"
                : order.getPaymentMethod() != null ? order.getPaymentMethod() : "Direct Payment";
        ctx.setVariable("paymentMethodDisplay", methodDisplay);
        ctx.setVariable("paymentStatus", order.getPaymentStatus());
        ctx.setVariable("paymentStatusColor", "PAID".equalsIgnoreCase(order.getPaymentStatus()) ? "#27ae60" : "#d35400");
    }

    private void populateAddressContext(Context ctx, ShippingAddress addr) {
        if (addr == null) {
            ctx.setVariable("hasShippingAddress", false);
            return;
        }
        ctx.setVariable("hasShippingAddress", true);
        ctx.setVariable("recipientName", addr.getFullName() != null ? addr.getFullName() : "");
        ctx.setVariable("addressLine1", addr.getAddressLine1() != null ? addr.getAddressLine1() : "");
        ctx.setVariable("addressLine2", addr.getAddressLine2());
        ctx.setVariable("city", addr.getCity() != null ? addr.getCity() : "");
        ctx.setVariable("state", addr.getState() != null ? addr.getState() : "");
        ctx.setVariable("postalCode", addr.getPostalCode() != null ? addr.getPostalCode() : "");
        ctx.setVariable("country", addr.getCountry() != null ? addr.getCountry() : "India");
        ctx.setVariable("phoneNumber", addr.getPhoneNumber() != null ? addr.getPhoneNumber() : "");
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return INR_FORMAT.format(amount);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "******";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) return name.charAt(0) + "***@" + domain;
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemView {
        private String productName;
        private String variantInfo;
        private int quantity;
        private BigDecimal unitPrice;
        private String unitPriceFormatted;
        private BigDecimal totalPrice;
        private String totalPriceFormatted;
    }
}