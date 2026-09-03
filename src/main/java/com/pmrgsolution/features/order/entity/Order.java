package com.pmrgsolution.features.order.entity;

import com.pmrgsolution.features.address.entity.ShippingAddress;
import com.pmrgsolution.features.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_order_number", columnList = "order_number"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", length = 50, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id")
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "cod_handling_fee", precision = 10, scale = 2)
    private BigDecimal codHandlingFee;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // RAZORPAY, COD, DIRECT_UPI

    @Column(name = "payment_status", length = 50)
    private String paymentStatus; // PENDING, PAID, FAILED, REFUNDED

    @Column(name = "status", length = 50)
    private String status; // PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "courier_partner")
    private String courierPartner;

    @Column(name = "tracking_url", columnDefinition = "TEXT")
    private String trackingUrl;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "notes")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "is_stock_deducted")
    @Builder.Default
    private Boolean isStockDeducted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    public BigDecimal getSubtotal() {
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public BigDecimal getDeliveryFee() {
        return shippingFee != null ? shippingFee : BigDecimal.ZERO;
    }

    /**
     * Returns the courier partner name, falling back to a default if not set.
     * NOTE: trackingUrl and estimatedDeliveryDate are intentionally NOT overridden here —
     * Lombok @Getter returns the actual DB-stored values set by admins.
     */
    public String getCourierName() {
        return courierPartner != null ? courierPartner : "Standard Courier";
    }

    public List<OrderItem> getOrderItems() {
        return items != null ? items : java.util.Collections.emptyList();
    }
}