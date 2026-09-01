package com.pmrgsolution.features.payment.entity;

import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_tx_order_id", columnList = "order_id"),
        @Index(name = "idx_tx_user_id", columnList = "user_id"),
        @Index(name = "idx_tx_utr", columnList = "utr_number"),
        @Index(name = "idx_tx_rp_order", columnList = "razorpay_order_id"),
        @Index(name = "idx_tx_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "razorpay_order_id", unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", columnDefinition = "TEXT")
    private String razorpaySignature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "utr_number", unique = true)
    private String utrNumber;

    @Column(name = "payment_proof")
    private byte[] paymentProof;

    @Column(name = "payment_proof_content_type")
    private String paymentProofContentType;

    @Column(name = "payment_proof_url", columnDefinition = "TEXT")
    private String paymentProofUrl;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "coupon_code") 
    private String couponCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}