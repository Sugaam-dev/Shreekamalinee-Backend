package com.pmrgsolution.features.order.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Immutable value object capturing all data required for transactional emails.
 * Built synchronously inside a @Transactional method to avoid Hibernate lazy-load
 * issues when passed to @Async email worker threads.
 *
 * Rule: This class MUST contain ONLY plain Java types — no JPA entities or proxies.
 */
@Value
@Builder
public class OrderEmailContext {

    // ---- Identity ----
    UUID orderId;
    String orderNumber;
    String orderTrackingUrl;

    // ---- Customer ----
    String customerEmail;
    String customerFirstName;
    String customerFullName;
    String customerEmailDisplay; // full name + email for admin views

    // ---- Pricing (all pre-formatted) ----
    BigDecimal subtotal;
    BigDecimal discountAmount;
    BigDecimal shippingFee;
    BigDecimal codHandlingFee;
    BigDecimal finalAmount;
    String subtotalFormatted;
    String discountFormatted;
    String shippingFormatted;
    String codFeeFormatted;
    String finalTotalFormatted;
    String formulaText;
    String couponCode;

    // ---- Payment ----
    String paymentMethod;          // raw: COD, RAZORPAY, MANUAL, DIRECT_UPI etc.
    String paymentMethodDisplay;   // human-readable
    String paymentStatus;          // raw: PENDING, PAID, FAILED
    String paymentStatusColor;     // hex for email templates

    // ---- UPI proof ----
    String utrNumber;

    // ---- Order state ----
    String orderStatus;            // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    String cancellationReason;
    boolean refundApplicable;

    // ---- Shipping / tracking ----
    String courierPartner;
    String trackingNumber;
    String trackingUrl;
    String estimatedDeliveryDateDisplay; // pre-formatted string for email

    // ---- Shipping address ----
    String recipientName;
    String addressLine1;
    String addressLine2;
    String city;
    String state;
    String postalCode;
    String country;
    String phoneNumber;
    boolean hasShippingAddress;

    // ---- City/State display for admin ----
    String customerCityState;

    // ---- Admin portal link ----
    String adminOrderUrl;

    // ---- Order items (plain POJOs) ----
    List<OrderItemLine> orderItems;

    // =========================================================================
    // Nested plain POJO — no JPA
    // =========================================================================
    @Value
    @Builder
    public static class OrderItemLine {
        String productName;
        String variantInfo;
        int quantity;
        BigDecimal unitPrice;
        String unitPriceFormatted;
        BigDecimal totalPrice;
        String totalPriceFormatted;
    }

    // =========================================================================
    // FACTORY METHOD — call this INSIDE your @Transactional method
    // All lazy-loaded associations must be eagerly resolved here.
    // =========================================================================
    public static OrderEmailContext from(
            com.pmrgsolution.features.order.entity.Order order,
            List<com.pmrgsolution.features.order.entity.OrderItem> items) {

        final String SUPPORT_URL = "https://www.shreekamalinee.com";
        final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // ---- Order number ----
        String orderNumber = (order.getOrderNumber() != null && !order.getOrderNumber().isBlank())
                ? order.getOrderNumber()
                : "SK-" + order.getId().toString().substring(0, 8).toUpperCase();

        // ---- Customer ----
        String firstName = "Valued Patron";
        String fullName = "Valued Patron";
        String customerEmail = "";
        if (order.getUser() != null) {
            firstName = order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "Valued Patron";
            String ln = order.getUser().getLastName() != null ? order.getUser().getLastName() : "";
            fullName = (firstName + " " + ln).trim();
            customerEmail = order.getUser().getEmail() != null ? order.getUser().getEmail() : "";
        }

        // ---- Pricing ----
        BigDecimal subtotal = orZero(order.getSubtotal());
        BigDecimal discount = orZero(order.getDiscountAmount());
        BigDecimal shipping = orZero(order.getShippingFee());
        BigDecimal codFee = orZero(order.getCodHandlingFee());
        BigDecimal finalAmt = order.getFinalAmount() != null
                ? order.getFinalAmount()
                : subtotal.subtract(discount).add(shipping).add(codFee);
        if (finalAmt.compareTo(BigDecimal.ZERO) < 0) finalAmt = BigDecimal.ZERO;

        StringBuilder formula = new StringBuilder("Subtotal ").append(fmt(INR, subtotal));
        if (discount.compareTo(BigDecimal.ZERO) > 0) formula.append(" - Discount ").append(fmt(INR, discount));
        formula.append(shipping.compareTo(BigDecimal.ZERO) > 0
                ? " + Shipping " + fmt(INR, shipping)
                : " + Shipping ₹0.00 (FREE)");
        if (codFee.compareTo(BigDecimal.ZERO) > 0) formula.append(" + COD Fee ").append(fmt(INR, codFee));
        formula.append(" = ").append(fmt(INR, finalAmt));

        // ---- Payment display ----
        String payMethod = order.getPaymentMethod();
        String payDisplay = resolvePaymentDisplay(payMethod);
        String payStatus = order.getPaymentStatus() != null ? order.getPaymentStatus() : "PENDING";
        String payColor = "PAID".equalsIgnoreCase(payStatus) ? "#27ae60" : "#d35400";

        // ---- Refund logic ----
        boolean refundApplicable = "PAID".equalsIgnoreCase(payStatus)
                || !"COD".equalsIgnoreCase(payMethod);

        // ---- Address ----
        String recipientName = "", line1 = "", line2 = "", city = "", state = "",
                postal = "", country = "India", phone = "", cityState = "India";
        boolean hasAddr = false;
        if (order.getShippingAddress() != null) {
            var addr = order.getShippingAddress();
            hasAddr = true;
            recipientName = nvl(addr.getFullName());
            line1 = nvl(addr.getAddressLine1());
            line2 = addr.getAddressLine2();
            city = nvl(addr.getCity());
            state = nvl(addr.getState());
            postal = nvl(addr.getPostalCode());
            country = addr.getCountry() != null ? addr.getCountry() : "India";
            phone = nvl(addr.getPhoneNumber());
            cityState = city + (state.isBlank() ? "" : ", " + state);
        }

        // ---- Tracking ----
        String estDateDisplay = "3-5 Business Days";
        if (order.getEstimatedDeliveryDate() != null) {
            try {
                estDateDisplay = order.getEstimatedDeliveryDate()
                        .format(DateTimeFormatter.ofPattern("dd MMMM, yyyy"));
            } catch (Exception ignored) {}
        }

        String trackingUrl = order.getTrackingUrl() != null && !order.getTrackingUrl().isBlank()
                ? order.getTrackingUrl() : SUPPORT_URL + "/account/orders";
        String trackingNum = order.getTrackingNumber() != null
                ? order.getTrackingNumber()
                : "AWB" + order.getId().toString().substring(0, 8).toUpperCase();
        String courierPartner = order.getCourierName();

        // ---- Order items ----
        List<OrderItemLine> itemLines = new ArrayList<>();
        if (items != null) {
            for (var item : items) {
                String productName = "Handloom Saree";
                String variantInfo = "";
                // Product name — stored directly on item if available
                if (item.getProduct() != null && item.getProduct().getName() != null) {
                    productName = item.getProduct().getName();
                } else if (item.getVariant() != null
                        && item.getVariant().getProduct() != null
                        && item.getVariant().getProduct().getName() != null) {
                    productName = item.getVariant().getProduct().getName();
                }
                // Variant info — prefer denormalised size/color columns on OrderItem
                if (item.getSize() != null || item.getColor() != null) {
                    variantInfo = (nvl(item.getSize()) + " " + nvl(item.getColor())).trim();
                } else if (item.getVariant() != null) {
                    variantInfo = (nvl(item.getVariant().getSize()) + " "
                            + nvl(item.getVariant().getColor())).trim();
                }
                BigDecimal unit = orZero(item.getPrice());
                BigDecimal total = orZero(item.getTotalPrice());
                if (total.compareTo(BigDecimal.ZERO) == 0 && item.getQuantity() > 0) {
                    total = unit.multiply(BigDecimal.valueOf(item.getQuantity()));
                }
                itemLines.add(OrderItemLine.builder()
                        .productName(productName)
                        .variantInfo(variantInfo)
                        .quantity(item.getQuantity())
                        .unitPrice(unit)
                        .unitPriceFormatted(fmt(INR, unit))
                        .totalPrice(total)
                        .totalPriceFormatted(fmt(INR, total))
                        .build());
            }
        }

        // utrNumber is checked using reflection or just fallback to null if not available
        String utr = null;
        try {
            utr = (String) order.getClass().getMethod("getUtrNumber").invoke(order);
        } catch(Exception e) {
            // Field not present, ignore
        }

        return OrderEmailContext.builder()
                // identity
                .orderId(order.getId())
                .orderNumber(orderNumber)
                .orderTrackingUrl(SUPPORT_URL + "/account/orders")
                .adminOrderUrl(SUPPORT_URL + "/admin/orders/" + order.getId())
                // customer
                .customerEmail(customerEmail)
                .customerFirstName(firstName)
                .customerFullName(fullName)
                .customerEmailDisplay(fullName + " <" + customerEmail + ">")
                // pricing
                .subtotal(subtotal)
                .discountAmount(discount)
                .shippingFee(shipping)
                .codHandlingFee(codFee)
                .finalAmount(finalAmt)
                .subtotalFormatted(fmt(INR, subtotal))
                .discountFormatted(fmt(INR, discount))
                .shippingFormatted(fmt(INR, shipping))
                .codFeeFormatted(fmt(INR, codFee))
                .finalTotalFormatted(fmt(INR, finalAmt))
                .formulaText(formula.toString())
                .couponCode(order.getCouponCode())
                // payment
                .paymentMethod(payMethod)
                .paymentMethodDisplay(payDisplay)
                .paymentStatus(payStatus)
                .paymentStatusColor(payColor)
                // utr
                .utrNumber(utr)
                // state
                .orderStatus(order.getStatus())
                .cancellationReason(order.getCancellationReason())
                .refundApplicable(refundApplicable)
                // tracking
                .courierPartner(courierPartner)
                .trackingNumber(trackingNum)
                .trackingUrl(trackingUrl)
                .estimatedDeliveryDateDisplay(estDateDisplay)
                // address
                .hasShippingAddress(hasAddr)
                .recipientName(recipientName)
                .addressLine1(line1)
                .addressLine2(line2)
                .city(city)
                .state(state)
                .postalCode(postal)
                .country(country)
                .phoneNumber(phone)
                .customerCityState(cityState)
                // items
                .orderItems(itemLines)
                .build();
    }

    // ---- private helpers ----
    private static BigDecimal orZero(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private static String nvl(String s) { return s != null ? s : ""; }
    private static String fmt(NumberFormat f, BigDecimal v) { return v != null ? f.format(v) : "₹0.00"; }
    private static String resolvePaymentDisplay(String method) {
        if (method == null) return "Direct Payment";
        return switch (method.toUpperCase()) {
            case "COD" -> "Cash on Delivery (COD)";
            case "RAZORPAY" -> "Online Payment (Razorpay)";
            case "WHATSAPP_UPI" -> "WhatsApp Direct UPI";
            case "DIRECT_BANK" -> "Direct Bank Transfer (NEFT/RTGS)";
            case "MANUAL" -> "Manual / Admin Created Order";
            case "DIRECT_UPI" -> "Direct UPI Transfer";
            default -> method;
        };
    }
}
