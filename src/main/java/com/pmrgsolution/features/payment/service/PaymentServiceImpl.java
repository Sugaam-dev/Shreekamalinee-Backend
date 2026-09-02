package com.pmrgsolution.features.payment.service;

import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.features.order.dto.OrderEmailContext;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.order.entity.Order;
import com.pmrgsolution.features.order.entity.OrderItem;
import com.pmrgsolution.features.order.repository.OrderItemRepository;
import com.pmrgsolution.features.order.repository.OrderRepository;
import com.pmrgsolution.features.payment.dto.PaymentVerificationRequest;
import com.pmrgsolution.features.payment.dto.TransactionResponse;
import com.pmrgsolution.features.payment.entity.Transaction;
import com.pmrgsolution.features.payment.repository.TransactionRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import com.pmrgsolution.core.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

import com.pmrgsolution.features.auth.service.EmailService;

import com.pmrgsolution.features.coupon.entity.CouponUsage;
import com.pmrgsolution.features.coupon.repository.CouponRepository;
import com.pmrgsolution.features.coupon.repository.CouponUsageRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TransactionRepository transactionRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final com.pmrgsolution.features.order.service.OrderService orderService;

    @Value("${razorpay.key.id:rzp_test_placeholder}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:rzp_test_secret_placeholder}")
    private String razorpayKeySecret;

    @Value("${app.admin.notification-email:${app.admin.email:admin@shreekamalinee.com}}")
    private String adminEmail;

    @Override
    @Transactional
    public OrderResponse createRazorpayOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new BusinessException("Order is already paid", HttpStatus.BAD_REQUEST);
        }

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", order.getFinalAmount().multiply(BigDecimal.valueOf(100)).intValue()); // in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getOrderNumber());

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(orderRequest);
            String rzpOrderId = rzpOrder.get("id");

            return OrderResponse.builder()
                    .id(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .finalAmount(order.getFinalAmount())
                    .razorpayOrderId(rzpOrderId)
                    .razorpayKeyId(razorpayKeyId)
                    .amount(order.getFinalAmount())
                    .currency("INR")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for order {}: {}", order.getOrderNumber(), e.getMessage());
            throw new BusinessException("Payment gateway initialization failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void verifyRazorpayPayment(PaymentVerificationRequest request, UUID userId) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            if (!isValid) {
                throw new BusinessException("Invalid Razorpay payment signature", HttpStatus.BAD_REQUEST);
            }

            // SECURITY FIX: Use findByIdAndUserId to ensure the caller owns this order.
            // Previously findById was used, allowing any user to mark any order as PAID.
            Order order = null;
            if (request.getOrderId() != null) {
                order = orderRepository.findByIdAndUserId(request.getOrderId(), userId).orElse(null);
                if (order == null) {
                    throw new BusinessException("Order not found or you do not have permission to complete this payment.", HttpStatus.FORBIDDEN);
                }
            }

            if (order != null) {
                order.setPaymentStatus("PAID");
                order.setStatus("CONFIRMED");
                Order savedOrder = orderRepository.save(order);

                // Deduct stock upon verified Razorpay online payment
                orderService.deductOrderStock(savedOrder);

                Transaction txn = Transaction.builder()
                        .order(savedOrder)
                        .transactionId(request.getRazorpayPaymentId())
                        .gateway("RAZORPAY")
                        .amount(savedOrder.getFinalAmount())
                        .status("SUCCESS")
                        .paymentMethod(savedOrder.getPaymentMethod())
                        .build();
                transactionRepository.save(txn);

                if (savedOrder.getCouponCode() != null && !savedOrder.getCouponCode().isBlank() && savedOrder.getUser() != null) {
                    couponRepository.findByCodeIgnoreCase(savedOrder.getCouponCode().trim()).ifPresent(coupon -> {
                        if (couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), savedOrder.getUser().getId()) == 0) {
                            coupon.setTimesUsed(coupon.getTimesUsed() + 1);
                            couponRepository.save(coupon);

                            CouponUsage usage = CouponUsage.builder()
                                    .coupon(coupon)
                                    .user(savedOrder.getUser())
                                    .orderId(savedOrder.getId())
                                    .build();
                            couponUsageRepository.save(usage);
                        }
                    });
                }

                OrderEmailContext rzpEmailCtx = buildEmailContext(savedOrder);
                if (savedOrder.getUser() != null && savedOrder.getUser().getEmail() != null) {
                    try {
                        emailService.sendOrderConfirmationEmail(savedOrder.getUser().getEmail(), rzpEmailCtx);
                    } catch (Exception e) {
                        log.warn("Failed to send Razorpay customer order confirmation email: {}", e.getMessage());
                    }
                }

                try {
                    if (adminEmail != null && !adminEmail.isBlank()) {
                        emailService.sendAdminNewOrderAlert(adminEmail, rzpEmailCtx);
                        if (savedOrder.getFinalAmount() != null && savedOrder.getFinalAmount().compareTo(BigDecimal.valueOf(25000)) >= 0) {
                            emailService.sendAdminVipOrderAlert(adminEmail, rzpEmailCtx);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to dispatch admin Razorpay new order alert: {}", e.getMessage());
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Error verifying payment signature: {}", e.getMessage());
            throw new BusinessException("Payment verification failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional
    public TransactionResponse submitManualPayment(UUID orderId, String utrNumber, MultipartFile receipt, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (utrNumber != null && !utrNumber.isBlank()) {
            String cleanUtr = utrNumber.trim();
            transactionRepository.findByUtrNumber(cleanUtr).ifPresent(existingTxn -> {
                if (existingTxn.getOrder() != null && !existingTxn.getOrder().getId().equals(order.getId())) {
                    throw new BusinessException("This UTR / Transaction ID has already been submitted for another order. Please check and enter your valid 12-digit UTR.", HttpStatus.CONFLICT);
                }
            });
        }

        String proofUrl = null;
        if (receipt != null && !receipt.isEmpty()) {
            try {
                proofUrl = fileStorageService.storeFile(receipt, "payment-proofs");
            } catch (Exception e) {
                log.error("Failed to store payment proof image for order {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }

        order.setPaymentMethod("MANUAL");
        order.setPaymentStatus("PENDING");           // stays PENDING until admin approves
        order.setStatus("PAYMENT_PROOF_SUBMITTED");  // signals admin that proof is awaiting review
        orderRepository.save(order);

        // Deduct inventory when customer actually uploads receipt and submits payment proof
        orderService.deductOrderStock(order);

        Transaction txn = transactionRepository.findByOrderId(order.getId())
                .orElseGet(() -> Transaction.builder()
                        .order(order)
                        .amount(order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount())
                        .build());

        txn.setUtrNumber(utrNumber != null ? utrNumber.trim() : null);
        if (proofUrl != null) {
            txn.setPaymentProofUrl(proofUrl);
        }
        txn.setGateway("MANUAL_UPI");
        txn.setPaymentMethod("MANUAL");
        txn.setStatus("PENDING");
        txn.setTransactionId(utrNumber != null ? utrNumber.trim() : "MANUAL-" + System.currentTimeMillis());

        Transaction savedTxn = transactionRepository.save(txn);

        if (order.getCouponCode() != null && !order.getCouponCode().isBlank() && order.getUser() != null) {
            couponRepository.findByCodeIgnoreCase(order.getCouponCode().trim()).ifPresent(coupon -> {
                if (couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), order.getUser().getId()) == 0) {
                    coupon.setTimesUsed(coupon.getTimesUsed() + 1);
                    couponRepository.save(coupon);

                    CouponUsage usage = CouponUsage.builder()
                            .coupon(coupon)
                            .user(order.getUser())
                            .orderId(order.getId())
                            .build();
                    couponUsageRepository.save(usage);
                }
            });
        }

        OrderEmailContext proofEmailCtx = buildEmailContext(order);
        if (order.getUser() != null && order.getUser().getEmail() != null) {
            try {
                emailService.sendPaymentProofReceivedEmail(order.getUser().getEmail(), proofEmailCtx);
            } catch (Exception e) {
                log.warn("Failed to dispatch customer payment proof received email: {}", e.getMessage());
            }
        }

        try {
            emailService.sendAdminManualPaymentUploadedAlert(adminEmail, proofEmailCtx);
        } catch (Exception e) {
            log.warn("Failed to dispatch admin manual payment alert: {}", e.getMessage());
        }

        return TransactionResponse.builder()
                .id(savedTxn.getId())
                .orderId(order.getId())
                .transactionId(savedTxn.getTransactionId())
                .gateway(savedTxn.getGateway())
                .amount(savedTxn.getAmount())
                .status(savedTxn.getStatus())
                .paymentMethod(savedTxn.getPaymentMethod())
                .createdAt(savedTxn.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByOrderId(UUID orderId, UUID userId) {
        // SECURITY FIX: Validate the caller owns the order before returning transaction data
        orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied."));

        return transactionRepository.findByOrderId(orderId)
                .map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .orderId(t.getOrder().getId())
                        .transactionId(t.getTransactionId())
                        .gateway(t.getGateway())
                        .amount(t.getAmount())
                        .status(t.getStatus())
                        .paymentMethod(t.getPaymentMethod())
                        .createdAt(t.getCreatedAt())
                        .build())
                .orElse(null);
    }

    private OrderEmailContext buildEmailContext(Order order) {
        java.util.List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return OrderEmailContext.from(order, items);
    }
}