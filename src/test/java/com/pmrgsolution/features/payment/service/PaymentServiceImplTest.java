package com.pmrgsolution.features.payment.service;

import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.core.service.FileStorageService;
import com.pmrgsolution.features.auth.service.EmailService;
import com.pmrgsolution.features.order.entity.Order;
import com.pmrgsolution.features.order.repository.OrderRepository;
import com.pmrgsolution.features.payment.dto.PaymentVerificationRequest;
import com.pmrgsolution.features.payment.dto.TransactionResponse;
import com.pmrgsolution.features.payment.entity.Transaction;
import com.pmrgsolution.features.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private com.pmrgsolution.features.order.repository.OrderItemRepository orderItemRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private EmailService emailService;
    @Mock private com.pmrgsolution.features.coupon.repository.CouponRepository couponRepository;
    @Mock private com.pmrgsolution.features.coupon.repository.CouponUsageRepository couponUsageRepository;
    @Mock private com.pmrgsolution.features.order.service.OrderService orderService;

    @InjectMocks private PaymentServiceImpl paymentService;

    private UUID userId;
    private UUID orderId;
    private Order order;
    private final String testSecret = "test_rzp_secret_key_123";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", testSecret);
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_id");

        order = new Order();
        order.setId(orderId);
        order.setFinalAmount(BigDecimal.valueOf(1500));
        order.setPaymentMethod("RAZORPAY");
        order.setPaymentStatus("PENDING");
        order.setStatus("PENDING");
    }

    private String generateSignature(String orderId, String paymentId, String secret) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("verifyPayment_invalidSignature: Throws BAD_REQUEST on invalid signature")
    void verifyPayment_invalidSignature_throwsBadRequest() {
        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .orderId(orderId)
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .razorpaySignature("invalid_signature")
                .build();

        assertThatThrownBy(() -> paymentService.verifyRazorpayPayment(req, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid Razorpay payment signature");
    }

    @Test
    @DisplayName("verifyPayment_orderNotOwnedByUser: Throws FORBIDDEN when user does not own the order")
    void verifyPayment_orderNotOwnedByUser_throwsForbidden() {
        String rzpOrderId = "order_123";
        String rzpPaymentId = "pay_123";
        String validSig = generateSignature(rzpOrderId, rzpPaymentId, testSecret);

        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .orderId(orderId)
                .razorpayOrderId(rzpOrderId)
                .razorpayPaymentId(rzpPaymentId)
                .razorpaySignature(validSig)
                .build();

        // Caller does not own the order
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.verifyRazorpayPayment(req, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("verifyPayment_validSignature_marksPaid: Successfully marks order as PAID and CONFIRMED")
    void verifyPayment_validSignature_success() {
        String rzpOrderId = "order_123";
        String rzpPaymentId = "pay_123";
        String validSig = generateSignature(rzpOrderId, rzpPaymentId, testSecret);

        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .orderId(orderId)
                .razorpayOrderId(rzpOrderId)
                .razorpayPaymentId(rzpPaymentId)
                .razorpaySignature(validSig)
                .build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

        paymentService.verifyRazorpayPayment(req, userId);

        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        verify(orderRepository).save(order);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("getTransaction_wrongUser: Throws ResourceNotFoundException when caller does not own order")
    void getTransaction_wrongUser_throws() {
        UUID anotherUserId = UUID.randomUUID();
        when(orderRepository.findByIdAndUserId(orderId, anotherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getTransactionByOrderId(orderId, anotherUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
