	package com.PMRGSolution.RENAISSANCE.features.payment.entity;
	
	import com.PMRGSolution.RENAISSANCE.Constant.TierType;
	import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
	import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
	import jakarta.persistence.*;
	import lombok.*;
	
	import java.time.LocalDateTime;
	import java.util.UUID;
	
	@Entity
	@Table(name = "transactions")
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public class Transaction {
	
	    @Id
	    @GeneratedValue(strategy = GenerationType.UUID)
	    private UUID id;
	
	    @Column(unique = true, nullable = false)
	    private String razorpayOrderId;
	
	    private String razorpayPaymentId;
	    private String razorpaySignature;
	
	    @ManyToOne(fetch = FetchType.LAZY)
	    private User user;
	
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "package_uuid")
	    private com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package productPackage;
	
	    private Double amount;
	    private String currency;
	
	    @Enumerated(EnumType.STRING)
	    private TierType tier;
	
	    @Column(nullable = false)
	    private String status; // PENDING, SUCCESS, FAILED
	    
	    @Column(name = "coupon_code", nullable = true) 
	    private String couponCode;
	
	    private LocalDateTime createdAt;
	    private LocalDateTime updatedAt;
	    
	    
	
	    @PrePersist
	    protected void onCreate() {
	        createdAt = LocalDateTime.now();
	        updatedAt = LocalDateTime.now();
	    }
	
	    @PreUpdate
	    protected void onUpdate() {
	        updatedAt = LocalDateTime.now();
	    }
	}