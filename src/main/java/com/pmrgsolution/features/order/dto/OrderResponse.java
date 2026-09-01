package com.pmrgsolution.features.order.dto;

import com.pmrgsolution.features.address.dto.AddressResponse;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private AddressResponse shippingAddress;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal codHandlingFee;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String status;
    private String trackingNumber;
    private String courierPartner;
    private String trackingUrl;
    private LocalDateTime estimatedDeliveryDate;
    private String couponCode;
    private String notes;
    private String cancellationReason;
    private String utrNumber;
    private String paymentProofUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Razorpay fields when initiating gateway
    private String razorpayOrderId;
    private String razorpayKeyId;
    private BigDecimal amount;
    private String currency;

    public BigDecimal getSubtotal() {
        return totalAmount != null ? totalAmount : finalAmount;
    }

    public BigDecimal getDeliveryFee() {
        return shippingFee != null ? shippingFee : BigDecimal.ZERO;
    }
}