package com.PMRGSolution.RENAISSANCE.features.payment.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import com.PMRGSolution.RENAISSANCE.Exception.BusinessException;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.UserRepository;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import com.PMRGSolution.RENAISSANCE.features.catalog.repository.PackageRepository;
import com.PMRGSolution.RENAISSANCE.features.payment.dto.*;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.*;
import com.PMRGSolution.RENAISSANCE.features.payment.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final TransactionRepository transactionRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PackageRepository catalogPackageRepository;
    private final CouponRepository couponRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID userId) {
        // 1. Fetch MASTER Package from Catalog
        com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package pkg = catalogPackageRepository
                .findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal finalPrice = pkg.getPriceAmount();
        String appliedCode = null;

        // 2. Coupon Validation
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponValidationResponse validation = validateCoupon(request.getCouponCode(), null, finalPrice.doubleValue());
            if (validation.isValid()) {
                finalPrice = validation.getFinalPrice();
                appliedCode = request.getCouponCode().toUpperCase();
            }
        }

        try {
            int amountInPaise = finalPrice.multiply(new BigDecimal(100)).intValue();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + UUID.randomUUID().toString().substring(0, 8));

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            // 3. Save PENDING Transaction linked to the Catalog Package
            Transaction txn = Transaction.builder()
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .user(user)
                    .productPackage(pkg) 
                    .tier(pkg.getTierType()) 
                    .amount(finalPrice.doubleValue())
                    .currency("INR")
                    .status("PENDING")
                    .couponCode(appliedCode)
                    .build();
            
            transactionRepository.save(txn);

            return OrderResponse.builder()
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .amount(amountInPaise)
                    .currency("INR")
                    .keyId(razorpayKeyId)
                    .businessName("Renaissance Learning")
                    .build();

        } catch (Exception e) {
            log.error("Order Creation Failed", e);
            throw new BusinessException("Payment gateway unreachable.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

   @Override
@Transactional
public void verifyPayment(PaymentVerificationRequest request) {
    try {
        // 1. Prepare attributes for verification
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", request.getRazorpayOrderId());
        attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
        attributes.put("razorpay_signature", request.getRazorpaySignature());

        // 2. Cryptographic Signature Verification
        // This is where the RazorpayException is thrown
        boolean isValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        
        if (!isValid) {
            throw new BusinessException("Invalid payment signature. Transaction rejected.", HttpStatus.BAD_REQUEST);
        }

        // 3. Fetch and Update Transaction
        Transaction txn = transactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction record not found."));

        if ("SUCCESS".equals(txn.getStatus())) {
            log.info("Payment already verified for Order ID: {}", request.getRazorpayOrderId());
            return;
        }

        txn.setRazorpayPaymentId(request.getRazorpayPaymentId());
        txn.setRazorpaySignature(request.getRazorpaySignature());
        txn.setStatus("SUCCESS");
        transactionRepository.save(txn);

        // 4. Activate Subscriptions
        activateComboSubscription(txn);

    } catch (com.razorpay.RazorpayException e) {
        // This catches the "Unhandled exception type RazorpayException"
        log.error("Razorpay SDK Error during verification: {}", e.getMessage());
        throw new BusinessException("Payment gateway verification failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
        log.error("General Error during payment verification: {}", e.getMessage());
        throw new BusinessException("An unexpected error occurred during activation.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

    private void activateComboSubscription(Transaction txn) {
        com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package pkg = txn.getProductPackage();
        
        for (ExamCategory category : pkg.getAccessibleCategories()) {
            UserSubscription sub = subscriptionRepository.findActiveSubscription(
                    txn.getUser().getEmail(), category.getId(), LocalDateTime.now())
                    .orElse(UserSubscription.builder()
                            .user(txn.getUser())
                            .category(category)
                            .startDate(LocalDateTime.now())
                            .expiryDate(LocalDateTime.now())
                            .tier(TierType.UNIVERSAL_FREE)
                            .build());

            // Upgrade vs Renewal Logic
            LocalDateTime baseDate = (txn.getTier().getRank() > sub.getTier().getRank()) 
                                     ? LocalDateTime.now() 
                                     : (sub.getExpiryDate().isAfter(LocalDateTime.now()) ? sub.getExpiryDate() : LocalDateTime.now());

            sub.setExpiryDate(baseDate.plusMonths(pkg.getDurationInMonths()));
            sub.setTier(txn.getTier());
            sub.setProductPackage(pkg); // Used for Mock Limit check
            sub.setActive(true);
            
            subscriptionRepository.save(sub);
        }
    }

    @Override
    public CouponValidationResponse validateCoupon(String code, UUID categoryId, Double currentPrice) {
        var couponOpt = couponRepository.findByCouponCodeAndActiveTrue(code.toUpperCase());
        if (couponOpt.isEmpty()) return CouponValidationResponse.builder().valid(false).message("Invalid coupon.").build();

        Coupon coupon = couponOpt.get();
        BigDecimal originalPrice = BigDecimal.valueOf(currentPrice);

        if (originalPrice.compareTo(coupon.getMinPurchaseAmount()) < 0) {
            return CouponValidationResponse.builder().valid(false).message("Min spend required: ₹" + coupon.getMinPurchaseAmount()).build();
        }

        BigDecimal discount = "PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType()) 
                ? originalPrice.multiply(coupon.getDiscountValue().divide(new BigDecimal(100))) 
                : coupon.getDiscountValue();

        return CouponValidationResponse.builder()
                .valid(true)
                .discountAmount(discount)
                .finalPrice(originalPrice.subtract(discount).max(BigDecimal.ZERO))
                .message("Applied!")
                .build();
    }

    // --- ADMIN METHODS ---
    @Override public List<UserSubscriptionResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream().map(sub -> UserSubscriptionResponse.builder()
                .id(sub.getId()).userEmail(sub.getUser().getEmail())
                .categoryDisplayName(sub.getCategory().getDisplayName())
                .tier(sub.getTier().name()).expiryDate(sub.getExpiryDate()).active(sub.isActive()).build()).collect(Collectors.toList());
    }

    @Override public List<TransactionAdminResponse> getAllTransactions() {
        return transactionRepository.findAll().stream().map(txn -> TransactionAdminResponse.builder()
                .razorpayOrderId(txn.getRazorpayOrderId()).userEmail(txn.getUser().getEmail())
                .packageName(txn.getProductPackage() != null ? txn.getProductPackage().getName() : "Deleted Package")
                .amount(txn.getAmount()).status(txn.getStatus()).createdAt(txn.getCreatedAt()).build()).collect(Collectors.toList());
    }

    @Override @Transactional public void createCoupon(CouponRequest r) {
        couponRepository.save(Coupon.builder().couponCode(r.getCode().toUpperCase()).discountType(r.getDiscountType())
                .discountValue(BigDecimal.valueOf(r.getDiscountValue())).minPurchaseAmount(BigDecimal.valueOf(r.getMinPurchaseAmount()))
                .expiryDate(r.getExpiryDate()).active(r.isActive()).build());
    }

    @Override public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream().map(c -> CouponResponse.builder().id(c.getId()).code(c.getCouponCode())
                .discountType(c.getDiscountType()).discountValue(c.getDiscountValue().doubleValue()).active(c.isActive()).build()).toList();
    }

    @Override public void deleteCoupon(UUID id) { couponRepository.deleteById(id); }
}