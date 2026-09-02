package com.pmrgsolution.features.order.service;

import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.Constant.Role;
import com.pmrgsolution.Constant.AuthProvider;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.catalog.entity.Product;
import com.pmrgsolution.features.catalog.entity.ProductVariant;
import com.pmrgsolution.features.catalog.repository.ProductRepository;
import com.pmrgsolution.features.catalog.repository.ProductVariantRepository;
import com.pmrgsolution.features.catalog.service.ProductService;
import com.pmrgsolution.features.cart.repository.CartRepository;
import com.pmrgsolution.features.cart.repository.CartItemRepository;
import com.pmrgsolution.features.coupon.repository.CouponRepository;
import com.pmrgsolution.features.coupon.repository.CouponUsageRepository;
import com.pmrgsolution.features.coupon.service.CouponService;
import com.pmrgsolution.features.order.dto.CheckoutRequest;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.order.entity.Order;
import com.pmrgsolution.features.order.entity.OrderItem;
import com.pmrgsolution.features.order.repository.OrderItemRepository;
import com.pmrgsolution.features.order.repository.OrderRepository;
import com.pmrgsolution.features.address.entity.ShippingAddress;
import com.pmrgsolution.features.address.repository.ShippingAddressRepository;
import com.pmrgsolution.features.auth.service.EmailService;
import com.pmrgsolution.features.payment.repository.TransactionRepository;
import com.pmrgsolution.features.settings.service.StoreSettingsService;
import com.pmrgsolution.features.settings.dto.StoreSettingsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ShippingAddressRepository shippingAddressRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private ProductService productService;
    @Mock private UserRepository userRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private CouponUsageRepository couponUsageRepository;
    @Mock private CouponService couponService;
    @Mock private StoreSettingsService storeSettingsService;
    @Mock private EmailService emailService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private OrderServiceImpl orderService;

    private UUID userId;
    private UUID orderId;
    private UUID addressId;
    private UUID variantId;
    private UUID productId;

    private User testUser;
    private ShippingAddress shippingAddress;
    private Product product;
    private ProductVariant variant;
    private Order pendingOrder;
    private StoreSettingsResponse settings;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        orderId   = UUID.randomUUID();
        addressId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        productId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId).firstName("Priya").lastName("S")
                .email("priya@test.com").role(Role.USER)
                .enabled(true).provider(AuthProvider.LOCAL)
                .build();

        shippingAddress = ShippingAddress.builder()
                .id(addressId).fullName("Priya S")
                .addressLine1("123 MG Road").city("Pune").state("MH")
                .postalCode("411001").country("India")
                .build();

        variant = ProductVariant.builder()
                .id(variantId).size("M").color("Blue").stockQuantity(10).sku("SKU-M-BLUE").build();

        product = new Product();
        product.setId(productId);
        product.setName("Handloom Saree");
        product.setSku("SAREE-001");
        product.setOriginalPrice(BigDecimal.valueOf(2000));
        product.setOfferPrice(BigDecimal.valueOf(1500));
        product.setVariants(List.of(variant));

        pendingOrder = Order.builder()
                .id(orderId).status("PENDING").paymentStatus("PENDING")
                .paymentMethod("RAZORPAY").totalAmount(BigDecimal.valueOf(1500))
                .user(testUser).build();

        settings = new StoreSettingsResponse();
        settings.setFreeShippingThreshold(BigDecimal.valueOf(1499));
        settings.setStandardShippingFee(BigDecimal.valueOf(99));
        settings.setCodHandlingFee(BigDecimal.valueOf(50));
        settings.setIsFreeShippingPromoActive(false);
    }

    // ─────────────────── CREATE ORDER TESTS ────────────────────

    @Test
    @DisplayName("createOrder_directBuy_insufficientStock: Throws CONFLICT when stock is too low")
    void createOrder_directBuy_insufficientStock_throws() {
        variant.setStockQuantity(0); // No stock!

        CheckoutRequest.DirectItem di = new CheckoutRequest.DirectItem();
        di.setProductId(productId);
        di.setVariantId(variantId);
        di.setQuantity(1);

        CheckoutRequest req = new CheckoutRequest();
        req.setShippingAddressId(addressId);
        req.setPaymentMethod("RAZORPAY");
        req.setDirectItem(di);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(shippingAddressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(shippingAddress));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> orderService.createOrder(userId, req, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ─────────────────── CANCEL ORDER TESTS ────────────────────

    @Test
    @DisplayName("cancelOrder_pending: Cancels PENDING order and restores variant stock")
    void cancelOrder_pending_success_restoresStock() {
        pendingOrder.setIsStockDeducted(true);
        OrderItem oi = OrderItem.builder()
                .id(UUID.randomUUID()).order(pendingOrder)
                .product(product).variant(variant).quantity(2).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(oi));
        when(productVariantRepository.findByIdForUpdate(variantId)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any())).thenReturn(variant);
        when(orderRepository.save(any())).thenReturn(pendingOrder);

        // Should not throw
        assertThatCode(() -> orderService.cancelOrderCustomer(userId, orderId))
                .doesNotThrowAnyException();

        // Variant stock should be restored: 10 + 2 = 12
        assertThat(variant.getStock()).isEqualTo(12);
        verify(productVariantRepository).save(variant);
    }

    // ─────────────────── ADMIN MANUAL ORDER TESTS ────────────────────

    @Test
    @DisplayName("createAdminManualOrder_razorpayPayment_throwsBadRequest: Rejects Razorpay for manual admin orders")
    void createAdminManualOrder_razorpay_throwsBadRequest() {
        com.pmrgsolution.features.order.dto.AdminManualOrderRequest req = com.pmrgsolution.features.order.dto.AdminManualOrderRequest.builder()
                .productId(productId)
                .customerName("Rahul Sharma")
                .customerPhone("9876543210")
                .addressLine1("123 Street")
                .city("Mumbai")
                .state("MH")
                .postalCode("400001")
                .paymentMethod("RAZORPAY")
                .build();

        assertThatThrownBy(() -> orderService.createAdminManualOrder(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Razorpay");
    }

    @Test
    @DisplayName("cancelOrder_shipped: Throws BAD_REQUEST for SHIPPED order")
    void cancelOrder_shipped_throws() {
        pendingOrder.setStatus("SHIPPED");

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.cancelOrderCustomer(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("cancelOrder_notOwned: Throws ResourceNotFoundException for another user s order")
    void cancelOrder_notOwned_throws() {
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrderCustomer(userId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────── ORDER TRACKING SECURITY TEST ────────────────────

    @Test
    @DisplayName("getOrderByOrderNumberForUser_wrongUser: Throws for order owned by another user")
    void getOrderByOrderNumberForUser_wrongUser_throws() {
        UUID anotherUserId = UUID.randomUUID();
        pendingOrder.setOrderNumber("SK-1234567890-123");
        pendingOrder.setUser(testUser); // testUser owns this order

        when(orderRepository.findByOrderNumber("SK-1234567890-123")).thenReturn(Optional.of(pendingOrder));

        // anotherUserId should NOT be able to see this order
        assertThatThrownBy(() -> orderService.getOrderByOrderNumberForUser("SK-1234567890-123", anotherUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderByOrderNumberForUser_correctUser: Returns order for the owner")
    void getOrderByOrderNumberForUser_correctUser_returns() {
        pendingOrder.setOrderNumber("SK-1234567890-123");
        pendingOrder.setUser(testUser);

        when(orderRepository.findByOrderNumber("SK-1234567890-123")).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(transactionRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // Should not throw for the correct user
        assertThatCode(() -> orderService.getOrderByOrderNumberForUser("SK-1234567890-123", userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("createAdminManualOrder_success: Creates order without attaching address to user profile")
    void createAdminManualOrder_success_addressNotAttachedToUserProfile() {
        com.pmrgsolution.features.order.dto.AdminManualOrderRequest req = new com.pmrgsolution.features.order.dto.AdminManualOrderRequest();
        req.setCustomerEmail("manual@test.com");
        req.setCustomerName("Manual Patron");
        req.setCustomerPhone("9876543210");
        req.setAddressLine1("456 MG Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setProductId(productId);
        req.setQuantity(1);
        req.setPaymentMethod("WHATSAPP_UPI");
        req.setPaymentStatus("PAID");

        when(userRepository.findByEmailIgnoreCase("manual@test.com")).thenReturn(Optional.of(testUser));
        when(shippingAddressRepository.save(org.mockito.ArgumentMatchers.any(ShippingAddress.class))).thenAnswer(i -> {
            ShippingAddress sa = i.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(sa.getUser()).isNull();
            sa.setId(UUID.randomUUID());
            return sa;
        });
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(orderId);
            return o;
        });
        when(orderItemRepository.save(org.mockito.ArgumentMatchers.any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(transactionRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        OrderResponse res = orderService.createAdminManualOrder(req);
        org.assertj.core.api.Assertions.assertThat(res).isNotNull();
        org.assertj.core.api.Assertions.assertThat(res.getShippingAddress()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(res.getShippingAddress().getAddressLine1()).isEqualTo("456 MG Road");
    }
}
