package com.pmrgsolution.features.settings.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "support_email", length = 100)
    private String supportEmail;

    // --- Dynamic Shipping Configurations ---
    @Builder.Default
    @Column(name = "free_shipping_threshold", precision = 10, scale = 2)
    private BigDecimal freeShippingThreshold = BigDecimal.valueOf(1499.00);

    @Builder.Default
    @Column(name = "standard_shipping_fee", precision = 10, scale = 2)
    private BigDecimal standardShippingFee = BigDecimal.valueOf(99.00);

    @Builder.Default
    @Column(name = "cod_handling_fee", precision = 10, scale = 2)
    private BigDecimal codHandlingFee = BigDecimal.valueOf(99.00);

    @Builder.Default
    @Column(name = "is_free_shipping_promo_active", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isFreeShippingPromoActive = false;

    // --- Dynamic Top Announcement / Offer Bar ---
    @Builder.Default
    @Column(name = "is_announcement_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isAnnouncementActive = true;

    @Builder.Default
    @Column(name = "announcement_text", length = 500)
    private String announcementText = "✨ Festive Handloom Edit Live — Free Express Shipping on Orders Above ₹1,499 ✨";

    @Column(name = "announcements_json", columnDefinition = "TEXT")
    private String announcementsJson;

    @Builder.Default
    @Column(name = "announcement_link", length = 255)
    private String announcementLink = "/shop";

    // --- Dynamic Payment Method Gateways Toggles ---
    @Builder.Default
    @Column(name = "is_upi_payment_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isUpiPaymentActive = true;

    @Builder.Default
    @Column(name = "is_razorpay_payment_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isRazorpayPaymentActive = true;

    @Builder.Default
    @Column(name = "is_cod_payment_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isCodPaymentActive = true;

    // --- Dynamic Delivery SLA & Turnaround (Business Days) ---
    @Builder.Default
    @Column(name = "estimated_delivery_days_min")
    private Integer estimatedDeliveryDaysMin = 3;

    @Builder.Default
    @Column(name = "estimated_delivery_days_max")
    private Integer estimatedDeliveryDaysMax = 5;

    // --- Dynamic Return & Exchange Policy ---
    @Builder.Default
    @Column(name = "return_window_days")
    private Integer returnWindowDays = 7;

    @Builder.Default
    @Column(name = "is_return_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isReturnActive = true;

    @Builder.Default
    @Column(name = "return_policy_text", columnDefinition = "TEXT")
    private String returnPolicyText = "Hassle-free 7-day return & exchange policy. The item must be unused, unwashed with original tags and packaging intact.";

    @Builder.Default
    @Column(name = "delivery_policy_notice", columnDefinition = "TEXT")
    private String deliveryPolicyNotice = "★ A 360° unboxing video showing the sealed parcel and shipping label is strictly mandatory for any return, exchange, or transit damage claim.";

    @Builder.Default
    @Column(name = "contact_address", length = 255)
    private String contactAddress = "Shreekamalinee Studio, Atelier Heritage Lane, Varanasi, Uttar Pradesh 221001, India";

    @Builder.Default
    @Column(name = "operating_hours", length = 100)
    private String operatingHours = "Monday to Saturday: 10:00 AM – 7:00 PM IST";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}