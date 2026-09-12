package com.pmrgsolution.features.order.service;

import com.pmrgsolution.constant.AddressType;
import com.pmrgsolution.constant.OrderStatus;
import com.pmrgsolution.constant.PaymentMethod;
import com.pmrgsolution.constant.PaymentStatus;
import com.pmrgsolution.exception.BusinessException;
import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.features.address.dto.AddressResponse;
import com.pmrgsolution.features.address.entity.ShippingAddress;
import com.pmrgsolution.features.address.repository.ShippingAddressRepository;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.auth.service.EmailService;
import com.pmrgsolution.features.auth.service.EmailUsageService;
import com.pmrgsolution.features.cart.entity.Cart;
import com.pmrgsolution.features.cart.entity.CartItem;
import com.pmrgsolution.features.cart.repository.CartItemRepository;
import com.pmrgsolution.features.cart.repository.CartRepository;
import com.pmrgsolution.features.catalog.dto.ProductDTO;
import com.pmrgsolution.features.catalog.entity.Product;
import com.pmrgsolution.features.catalog.entity.ProductVariant;
import com.pmrgsolution.features.catalog.repository.ProductRepository;
import com.pmrgsolution.features.catalog.repository.ProductVariantRepository;
import com.pmrgsolution.features.catalog.service.ProductService;
import com.pmrgsolution.features.coupon.dto.CouponValidationResponse;
import com.pmrgsolution.features.coupon.entity.Coupon;
import com.pmrgsolution.features.coupon.entity.CouponUsage;
import com.pmrgsolution.features.coupon.repository.CouponRepository;
import com.pmrgsolution.features.coupon.repository.CouponUsageRepository;
import com.pmrgsolution.features.coupon.service.CouponService;
import com.pmrgsolution.features.order.dto.*;
import com.pmrgsolution.features.order.dto.OrderEmailContext;
import com.pmrgsolution.features.order.entity.Order;
import com.pmrgsolution.features.order.entity.OrderItem;
import com.pmrgsolution.features.order.repository.OrderItemRepository;
import com.pmrgsolution.features.order.repository.OrderRepository;
import com.pmrgsolution.features.settings.dto.StoreSettingsResponse;
import com.pmrgsolution.features.settings.service.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pmrgsolution.features.payment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponService couponService;
    private final StoreSettingsService storeSettingsService;
    private final EmailService emailService;
    private final EmailUsageService emailUsageService;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.pmrgsolution.core.service.RealtimeEventService realtimeEventService;
    private final com.pmrgsolution.core.service.FileStorageService fileStorageService;

    @Value("${app.admin.notification-email:${app.admin.email:admin@shreekamalinee.com}}")
    private String adminEmail;

    /** IDEMPOTENCY: Redis-backed key store with 24h TTL — survives restarts and works across multiple instances */
    private static final String IDEMPOTENCY_KEY_PREFIX = "idem:order:";
    private static final java.time.Duration IDEMPOTENCY_TTL = java.time.Duration.ofHours(24);

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public OrderResponse createOrder(UUID userId, CheckoutRequest request, String idempotencyKey) {
        // IDEMPOTENCY CHECK: Use Redis with graceful fallback if Redis is down
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                    String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
                    // PERF FIX: Use atomic setIfAbsent (Redis SETNX) for idempotency.
                    // The old hasKey() + set() pattern had a race window where two concurrent requests
                    // could both pass the hasKey() check before either one called set().
                    // setIfAbsent is atomic — only one caller wins, the other gets false.
                    Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(
                            redisKey, "1", java.time.Duration.ofMinutes(30));

                    if (Boolean.FALSE.equals(isNewRequest)) {
                        // Another request with the same idempotency key already won
                        log.warn("Duplicate order submission blocked by idempotency key: {}", idempotencyKey);
                        throw new BusinessException("This order has already been submitted. Please refresh your order history.", HttpStatus.CONFLICT);
                    }
                }
            } catch (BusinessException be) {
                throw be;
            } catch (Exception ex) {
                log.warn("Redis idempotency check skipped due to Redis unavailability: {}", ex.getMessage());
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShippingAddress shippingAddress = shippingAddressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        if ((user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) && shippingAddress.getPhoneNumber() != null && !shippingAddress.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(shippingAddress.getPhoneNumber().trim());
            userRepository.save(user);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartItem> purchasedCartItems = new ArrayList<>();
        boolean isDirectBuy = request.getDirectItem() != null && request.getDirectItem().getProductId() != null;

        Product directProduct = null;
        ProductVariant directVariant = null;
        int directQuantity = 1;

        if (isDirectBuy) {
            CheckoutRequest.DirectItem di = request.getDirectItem();
            directProduct = productRepository.findById(di.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            directQuantity = di.getQuantity() != null && di.getQuantity() > 0 ? di.getQuantity() : 1;

            if (di.getVariantId() != null) {
                directVariant = productVariantRepository.findById(di.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
                if (directVariant.getStock() < directQuantity) {
                    throw new BusinessException("Insufficient stock for product variant: " + directProduct.getName(), HttpStatus.CONFLICT);
                }
            } else if (directProduct.getStock() < directQuantity) {
                throw new BusinessException("Insufficient stock for product: " + directProduct.getName(), HttpStatus.CONFLICT);
            }

            BigDecimal itemPrice = directProduct.getOfferPrice() != null ? directProduct.getOfferPrice() : directProduct.getPrice();
            subtotal = itemPrice.multiply(BigDecimal.valueOf(directQuantity));
        } else {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("Cart is empty", HttpStatus.BAD_REQUEST));

            List<CartItem> allCartItems = cartItemRepository.findByCartId(cart.getId());
            if (allCartItems.isEmpty()) {
                throw new BusinessException("Cart is empty", HttpStatus.BAD_REQUEST);
            }

            if (request.getSelectedCartItemIds() != null && !request.getSelectedCartItemIds().isEmpty()) {
                purchasedCartItems = allCartItems.stream()
                        .filter(ci -> request.getSelectedCartItemIds().contains(ci.getId()))
                        .toList();
            } else {
                purchasedCartItems = allCartItems;
            }

            if (purchasedCartItems.isEmpty()) {
                throw new BusinessException("No items selected for checkout", HttpStatus.BAD_REQUEST);
            }

            for (CartItem ci : purchasedCartItems) {
                Product p = ci.getProduct();
                if (ci.getVariant() != null) {
                    ProductVariant variant = ci.getVariant();
                    if (variant.getStock() < ci.getQuantity()) {
                        throw new BusinessException("Insufficient stock for product variant: " + p.getName() + " (" + variant.getSize() + "/" + variant.getColor() + ")", HttpStatus.CONFLICT);
                    }
                } else if (p.getStock() < ci.getQuantity()) {
                    throw new BusinessException("Insufficient stock for product: " + p.getName(), HttpStatus.CONFLICT);
                }
                BigDecimal itemPrice = p.getOfferPrice() != null ? p.getOfferPrice() : p.getPrice();
                subtotal = subtotal.add(itemPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
            }
        }

        StoreSettingsResponse settings = storeSettingsService.getStoreSettings();

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponValidationResponse val = couponService.validateCoupon(request.getCouponCode().trim(), subtotal, userId);
            if (val.isValid()) {
                discountAmount = val.getCalculatedDiscount();
                appliedCoupon = couponRepository.findByCodeIgnoreCase(request.getCouponCode().trim()).orElse(null);
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal freeShippingThreshold = settings.getFreeShippingThreshold() != null ? settings.getFreeShippingThreshold() : BigDecimal.valueOf(1499.00);
        BigDecimal standardShippingFee = settings.getStandardShippingFee() != null ? settings.getStandardShippingFee() : BigDecimal.valueOf(99.00);

        if (Boolean.TRUE.equals(settings.getIsFreeShippingPromoActive()) || subtotal.compareTo(freeShippingThreshold) >= 0) {
            shippingFee = BigDecimal.ZERO;
        } else {
            shippingFee = standardShippingFee;
        }

        BigDecimal codHandlingFee = BigDecimal.ZERO;
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            BigDecimal baseCodFee = settings.getCodHandlingFee() != null ? settings.getCodHandlingFee() : BigDecimal.valueOf(99.00);
            BigDecimal freeCodThreshold = settings.getFreeCodThreshold() != null ? settings.getFreeCodThreshold() : BigDecimal.valueOf(2999.00);
            if (subtotal.compareTo(freeCodThreshold) >= 0) {
                codHandlingFee = BigDecimal.ZERO;
            } else {
                codHandlingFee = baseCodFee;
            }
        }

        BigDecimal finalAmount = subtotal.subtract(discountAmount).add(shippingFee).add(codHandlingFee);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            String randomPart = String.format("%04d", (int)(Math.random() * 9000 + 1000));
            orderNumber = "SK-" + datePart + "-" + randomPart;
        } while (orderRepository.findByOrderNumber(orderNumber).isPresent());

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(shippingAddress)
                .totalAmount(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .codHandlingFee(codHandlingFee)
                .finalAmount(finalAmount)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .status(OrderStatus.PLACED)
                .couponCode(appliedCoupon != null ? appliedCoupon.getCode() : null)
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);

        if (isDirectBuy) {
            BigDecimal itemPrice = directProduct.getOfferPrice() != null ? directProduct.getOfferPrice() : directProduct.getPrice();
            OrderItem oi = OrderItem.builder()
                    .order(savedOrder)
                    .product(directProduct)
                    .variant(directVariant)
                    .quantity(directQuantity)
                    .price(itemPrice)
                    .totalPrice(itemPrice.multiply(BigDecimal.valueOf(directQuantity)))
                    .size(request.getDirectItem().getSize() != null ? request.getDirectItem().getSize() : (directVariant != null ? directVariant.getSize() : null))
                    .color(request.getDirectItem().getColor() != null ? request.getDirectItem().getColor() : (directVariant != null ? directVariant.getColor() : null))
                    .build();
            orderItemRepository.save(oi);
        } else {
            for (CartItem ci : purchasedCartItems) {
                Product p = ci.getProduct();
                BigDecimal itemPrice = p.getOfferPrice() != null ? p.getOfferPrice() : p.getPrice();
                OrderItem oi = OrderItem.builder()
                        .order(savedOrder)
                        .product(p)
                        .variant(ci.getVariant())
                        .quantity(ci.getQuantity())
                        .price(itemPrice)
                        .totalPrice(itemPrice.multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .size(ci.getSize())
                        .color(ci.getColor())
                        .build();

                orderItemRepository.save(oi);
            }

            // Only delete the items that were purchased from the cart!
            cartItemRepository.deleteAll(purchasedCartItems);
        }

        // DEDUCT STOCK TIMING RULE:
        // Stock is deducted immediately ONLY for Cash On Delivery (COD) or instant pre-paid orders.
        // For Manual UPI / QR / Bank Transfer & Razorpay & WhatsApp, stock is NOT deducted on order initialization;
        // it is deducted when the user submits payment proof or Razorpay verification succeeds.
        if (savedOrder.getPaymentMethod() == PaymentMethod.COD || savedOrder.getPaymentStatus() == PaymentStatus.PAID) {
            deductOrderStock(savedOrder);
        }

        // Only record coupon usage if payment is confirmed (e.g. COD or immediate PAID)
        // For Manual UPI & Razorpay, coupon usage is recorded upon payment receipt upload / payment verification
        if (appliedCoupon != null && (savedOrder.getPaymentMethod() == PaymentMethod.COD || savedOrder.getPaymentStatus() == PaymentStatus.PAID)) {
            // SECURITY FIX: Use atomic DB-level increment to prevent race condition
            couponRepository.incrementTimesUsed(appliedCoupon.getId());

            CouponUsage usage = CouponUsage.builder()
                    .coupon(appliedCoupon)
                    .user(user)
                    .orderId(savedOrder.getId())
                    .build();
            couponUsageRepository.save(usage);
        }

        // Customer Confirmation Email (Only for confirmed COD or already PAID orders)
        if (savedOrder.getPaymentMethod() == PaymentMethod.COD || savedOrder.getPaymentStatus() == PaymentStatus.PAID) {
            try {
                emailService.sendOrderConfirmationEmail(user.getEmail(), buildEmailContext(savedOrder));
            } catch (Exception e) {
                log.warn("Failed to send order confirmation email: {}", e.getMessage());
            }
        }

        // Admin Notification Email Alert (Only for confirmed COD or already PAID orders — NOT for unverified pending UPI orders)
        if (savedOrder.getPaymentMethod() == PaymentMethod.COD || savedOrder.getPaymentStatus() == PaymentStatus.PAID) {
            try {
                if (adminEmail != null && !adminEmail.isBlank()) {
                    // Build context once and reuse for both admin alerts
                    OrderEmailContext orderCtx = buildEmailContext(savedOrder);
                    emailService.sendAdminNewOrderAlert(adminEmail, orderCtx);
                    if (savedOrder.getFinalAmount() != null && savedOrder.getFinalAmount().compareTo(BigDecimal.valueOf(25000)) >= 0) {
                        emailService.sendAdminVipOrderAlert(adminEmail, orderCtx);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to dispatch admin new order email alert: {}", e.getMessage());
            }
        }

        OrderResponse response = mapToResponse(savedOrder);
        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + savedOrder.getId() + "\",\"orderNumber\":\"" + savedOrder.getOrderNumber() + "\"}");
        realtimeEventService.broadcast("STOCK_UPDATED", "{\"type\":\"STOCK_UPDATED\"}");

        // Record idempotency key in Redis with TTL so future duplicates are rejected
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                    redisTemplate.opsForValue().set(
                        IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                        savedOrder.getId().toString(),
                        IDEMPOTENCY_TTL
                    );
                }
            } catch (Exception ex) {
                log.warn("Failed to record idempotency key in Redis: {}", ex.getMessage());
            }
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories", "adminDashboard"}, allEntries = true)
    public OrderResponse createAdminManualOrder(AdminManualOrderRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new BusinessException("Product selection is required for manual order", HttpStatus.BAD_REQUEST);
        }

        if (request.getPaymentMethod() == PaymentMethod.RAZORPAY) {
            throw new BusinessException("Razorpay online payment is not supported for manual admin orders. Please choose DIRECT_UPI, MANUAL, or COD.", HttpStatus.BAD_REQUEST);
        }

        // 1. Find or create User by email/phone
        String cleanEmail = request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()
                ? request.getCustomerEmail().trim().toLowerCase()
                : "guest_" + System.currentTimeMillis() + "@shreekamalinee.com";
        String cleanPhone = request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : "9820785210";
        String cleanName = request.getCustomerName() != null ? request.getCustomerName().trim() : "Patron Customer";

        String[] nameParts = cleanName.split(" ", 2);
        String fName = nameParts[0];
        String lName = nameParts.length > 1 ? nameParts[1] : "Patron";

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .firstName(fName)
                            .lastName(lName)
                            .email(cleanEmail)
                            .phoneNumber(cleanPhone)
                            .role(com.pmrgsolution.constant.Role.USER)
                            .enabled(true)
                            .accountNonLocked(true)
                            .provider(com.pmrgsolution.constant.AuthProvider.LOCAL)
                            // SECURITY FIX: Encode the random password with BCrypt.
                            // Previously a raw UUID string was stored, violating the
                            // encoding contract. The password is still random & unusable.
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .build();
                    return userRepository.save(newUser);
                });

        // 2. Create Shipping Address (Linked to user)
        ShippingAddress address = ShippingAddress.builder()
                .user(user)
                .fullName(cleanName)
                .phoneNumber(cleanPhone)
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country("India")
                .addressType(AddressType.MANUAL_ORDER)
                .isDefault(false)
                .build();
        address = shippingAddressRepository.save(address);

        // 3. Product & stock check
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        int qty = request.getQuantity() > 0 ? request.getQuantity() : 1;

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
            if (variant.getStock() < qty) {
                throw new BusinessException("Insufficient stock for variant: " + product.getName(), HttpStatus.CONFLICT);
            }
        } else if (product.getStock() < qty) {
            throw new BusinessException("Insufficient stock for product: " + product.getName(), HttpStatus.CONFLICT);
        }

        BigDecimal unitPrice = product.getOfferPrice() != null ? product.getOfferPrice() : product.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

        // 4. Coupon Validation & Consumption
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponValidationResponse val = couponService.validateCoupon(request.getCouponCode().trim(), subtotal, user.getId(), cleanEmail);
            if (val.isValid()) {
                discountAmount = val.getCalculatedDiscount() != null ? val.getCalculatedDiscount() : (val.getDiscountAmount() != null ? val.getDiscountAmount() : BigDecimal.ZERO);
                appliedCoupon = couponRepository.findByCodeIgnoreCase(request.getCouponCode().trim()).orElse(null);
            } else {
                log.warn("Coupon validation note for manual order: {}", val.getMessage());
            }
        }

        // Dynamic Shipping & COD Fee Calculation from Store Settings
        StoreSettingsResponse settings = storeSettingsService.getStoreSettings();
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal freeShippingThreshold = settings.getFreeShippingThreshold() != null ? settings.getFreeShippingThreshold() : BigDecimal.valueOf(1499.00);
        BigDecimal baseShippingFee = settings.getStandardShippingFee() != null ? settings.getStandardShippingFee() : BigDecimal.valueOf(99.00);

        if (Boolean.TRUE.equals(settings.getIsFreeShippingPromoActive()) || subtotal.compareTo(freeShippingThreshold) >= 0) {
            shippingFee = BigDecimal.ZERO;
        } else {
            shippingFee = baseShippingFee;
        }

        PaymentMethod payMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.UPI;
        BigDecimal codHandlingFee = BigDecimal.ZERO;
        if (payMethod == PaymentMethod.COD) {
            BigDecimal baseCodFee = settings.getCodHandlingFee() != null ? settings.getCodHandlingFee() : BigDecimal.valueOf(99.00);
            BigDecimal freeCodThreshold = settings.getFreeCodThreshold() != null ? settings.getFreeCodThreshold() : BigDecimal.valueOf(2999.00);
            if (subtotal.compareTo(freeCodThreshold) >= 0) {
                codHandlingFee = BigDecimal.ZERO;
            } else {
                codHandlingFee = baseCodFee;
            }
        }

        BigDecimal finalAmount = subtotal.subtract(discountAmount).add(shippingFee).add(codHandlingFee);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            String randomPart = String.format("%04d", (int)(Math.random() * 9000 + 1000));
            orderNumber = "SK-" + datePart + "-" + randomPart;
        } while (orderRepository.findByOrderNumber(orderNumber).isPresent());

        PaymentStatus payStatus = request.getPaymentStatus() != null ? request.getPaymentStatus() : PaymentStatus.PAID;
        OrderStatus orderStatus = (payStatus == PaymentStatus.PAID) ? OrderStatus.PROCESSING : OrderStatus.PLACED;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(address)
                .totalAmount(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .codHandlingFee(codHandlingFee)
                .finalAmount(finalAmount)
                .paymentMethod(payMethod)
                .paymentStatus(payStatus)
                .status(orderStatus)
                .couponCode(appliedCoupon != null ? appliedCoupon.getCode() : null)
                .notes(request.getNotes() != null ? request.getNotes() : "Manually booked via WhatsApp / Concierge by Admin")
                .build();

        order = orderRepository.save(order);

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .variant(variant)
                .quantity(qty)
                .price(unitPrice)
                .totalPrice(subtotal)
                .size(variant != null ? variant.getSize() : "Free Size")
                .color(variant != null ? variant.getColor() : "Standard")
                .build();

        orderItemRepository.save(item);
        deductOrderStock(order);

        // Record Coupon Usage
        if (appliedCoupon != null) {
            CouponUsage usage = CouponUsage.builder()
                    .coupon(appliedCoupon)
                    .user(user)
                    .orderId(order.getId())
                    .build();
            couponUsageRepository.save(usage);
            appliedCoupon.setTimesUsed(appliedCoupon.getTimesUsed() + 1);
            couponRepository.save(appliedCoupon);
        }

        // Record Transaction
        com.pmrgsolution.features.payment.entity.Transaction tx = com.pmrgsolution.features.payment.entity.Transaction.builder()
                .order(order)
                .user(user)
                .amount(finalAmount)
                .gateway(com.pmrgsolution.constant.PaymentGateway.DIRECT_UPI)
                .paymentMethod(payMethod)
                .status(payStatus)
                .transactionId("MANUAL-" + orderNumber)
                .utrNumber("WA-" + orderNumber)
                .couponCode(appliedCoupon != null ? appliedCoupon.getCode() : null)
                .build();
        transactionRepository.save(tx);

        // Send Email confirmation if requested
        if (Boolean.TRUE.equals(request.getSendEmailNotification()) && cleanEmail.contains("@") && !cleanEmail.startsWith("guest_")) {
            try {
                emailService.sendOrderConfirmationEmail(cleanEmail, buildEmailContext(order));
            } catch (Exception e) {
                log.warn("Failed to dispatch manual order confirmation email to {}: {}", cleanEmail, e.getMessage());
            }
        }

        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + order.getId() + "\",\"orderNumber\":\"" + order.getOrderNumber() + "\"}");
        realtimeEventService.broadcast("STOCK_UPDATED", "{\"type\":\"STOCK_UPDATED\"}");

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = null;
        try {
            UUID id = UUID.fromString(orderNumber);
            order = orderRepository.findById(id).orElse(null);
        } catch (IllegalArgumentException ignored) {}

        if (order == null) {
            order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with identifier: " + orderNumber));
        }
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumberForUser(String orderNumber, UUID userId) {
        // SECURITY FIX: Validate the authenticated user owns this order before returning any data.
        // Previously, the track endpoint was public and leaked full PII (name, address, phone)
        // to anyone who could guess the predictable order number format (SK-{timestamp}-{100-999}).
        Order order = null;
        try {
            UUID id = UUID.fromString(orderNumber);
            order = orderRepository.findByIdAndUserId(id, userId).orElse(null);
        } catch (IllegalArgumentException ignored) {}

        if (order == null) {
            order = orderRepository.findByOrderNumber(orderNumber)
                    .filter(o -> o.getUser() != null && userId.equals(o.getUser().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found or you do not have permission to view this order."));
        }
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrderCustomer(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PAYMENT_PROOF_SUBMITTED) {
            throw new BusinessException("Order cannot be cancelled in status: " + order.getStatus(), HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }

        restoreOrderStock(order);

        Order saved = orderRepository.save(order);
        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + saved.getId() + "\",\"orderNumber\":\"" + saved.getOrderNumber() + "\"}");
        realtimeEventService.broadcast("STOCK_UPDATED", "{\"type\":\"STOCK_UPDATED\"}");
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersAdmin(String status, Pageable pageable) {
        Page<Order> page;
        OrderStatus orderStatusFilter = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? OrderStatus.fromString(status)
                : null;

        if (orderStatusFilter != null) {
            page = orderRepository.findByStatusOrderByCreatedAtDesc(orderStatusFilter, pageable);
        } else {
            page = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public OrderResponse adminUpdateOrderStatus(UUID orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus newStatus = request.getStatus();

        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            if (request.getCancellationReason() != null && !request.getCancellationReason().isBlank()) {
                order.setCancellationReason(request.getCancellationReason().trim());
            } else if (order.getCancellationReason() == null) {
                order.setCancellationReason("Cancelled by Store Administrator");
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            }
            restoreOrderStock(order);
            try {
                OrderEmailContext cancelCtx = buildEmailContext(order);
                emailService.sendOrderCancelledEmail(order.getUser().getEmail(), cancelCtx);
                if (adminEmail != null && !adminEmail.isBlank()) {
                    emailService.sendAdminOrderCancelledAlert(adminEmail, cancelCtx);
                }
            } catch (Exception e) {
                log.warn("Failed to send order cancellation email: {}", e.getMessage());
            }
        } else if (newStatus != OrderStatus.CANCELLED) {
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.SHIPPED) {
                try {
                    emailService.sendOrderShippedEmail(order.getUser().getEmail(), buildEmailContext(order));
                } catch (Exception e) {
                    log.warn("Failed to send order shipped email: {}", e.getMessage());
                }
            } else if (newStatus == OrderStatus.DELIVERED) {
                try {
                    emailService.sendOrderDeliveredEmail(order.getUser().getEmail(), buildEmailContext(order));
                } catch (Exception e) {
                    log.warn("Failed to send order delivered email: {}", e.getMessage());
                }
            }
        }

        Order saved = orderRepository.save(order);
        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + saved.getId() + "\",\"orderNumber\":\"" + saved.getOrderNumber() + "\"}");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse adminUpdateShippingDetails(UUID orderId, ShippingDetailsUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber().trim());
        }
        if (request.getCourierPartner() != null && !request.getCourierPartner().isBlank()) {
            order.setCourierPartner(request.getCourierPartner().trim());
        }
        if (request.getTrackingUrl() != null && !request.getTrackingUrl().isBlank()) {
            order.setTrackingUrl(request.getTrackingUrl().trim());
        }
        if (request.getEstimatedDeliveryDate() != null && !request.getEstimatedDeliveryDate().isBlank()) {
            try {
                String edStr = request.getEstimatedDeliveryDate().trim();
                if (edStr.length() == 10) {
                    order.setEstimatedDeliveryDate(java.time.LocalDate.parse(edStr).atTime(23, 59, 59));
                } else {
                    order.setEstimatedDeliveryDate(java.time.LocalDateTime.parse(edStr));
                }
            } catch (Exception e) {
                log.warn("Could not parse estimatedDeliveryDate '{}' for order {}", request.getEstimatedDeliveryDate(), orderId);
            }
        }

        Order saved = orderRepository.save(order);
        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + saved.getId() + "\",\"orderNumber\":\"" + saved.getOrderNumber() + "\"}");

        // If order is already SHIPPED and tracking was just added, re-send the shipped email
        // so the customer gets the courier tracking info
        if (saved.getStatus() == OrderStatus.SHIPPED && request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            try {
                emailService.sendOrderShippedEmail(saved.getUser().getEmail(), buildEmailContext(saved));
                log.info("Re-sent shipped email with tracking details for order {}", saved.getOrderNumber());
            } catch (Exception e) {
                log.warn("Failed to re-send shipped email after tracking update: {}", e.getMessage());
            }
        }

        return mapToResponse(saved);
    }

    /**
     * Builds an immutable {@link OrderEmailContext} from a live Order entity.
     * MUST be called within an active @Transactional scope so all lazy associations
     * (user, shippingAddress) can be accessed safely before the async thread fires.
     *
     * @param order the saved Order entity
     * @return fully-resolved, thread-safe email context DTO
     */
    private OrderEmailContext buildEmailContext(Order order) {
        // Eagerly fetch items via the dedicated repository query — avoids Hibernate
        // lazy-load bag initialization which causes the IllegalStateException crash
        // when accessed later in @Async email threads.
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return OrderEmailContext.from(order, items);
    }

    /**
     * Admin payment approval — handles all payment types:
     * - MANUAL / DIRECT_UPI: Admin verified UTR/screenshot → confirm payment
     * - COD: Delivery collected cash → mark as received
     * - MANUAL (admin-created order): Admin confirms payment added outside system
     *
     * Idempotent: calling twice on an already-PAID order is a no-op.
     */
    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public OrderResponse approveManualPaymentAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Idempotency guard — approving an already-PAID order is a no-op
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Payment approval skipped — order {} is already PAID", order.getOrderNumber());
            return mapToResponse(order);
        }

        // Mark payment as received
        order.setPaymentStatus(PaymentStatus.PAID);

        // Advance order status:
        // PAYMENT_PROOF_SUBMITTED or PLACED → CONFIRMED
        // COD orders in SHIPPED/DELIVERED stay in their delivery status — only paymentStatus changes
        OrderStatus currentStatus = order.getStatus() != null ? order.getStatus() : OrderStatus.PLACED;
        if (currentStatus == OrderStatus.PAYMENT_PROOF_SUBMITTED || currentStatus == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        // For COD: SHIPPED/DELIVERED orders keep their status; only paymentStatus changes to PAID

        // Update linked transaction record to SUCCESS
        try {
            transactionRepository.findByOrderId(order.getId()).ifPresent(tx -> {
                tx.setStatus(PaymentStatus.SUCCESS);
                transactionRepository.save(tx);
            });
        } catch (Exception e) {
            log.warn("Could not update transaction status for order {}: {}", orderId, e.getMessage());
        }

        // Deduct inventory — idempotent (guarded by isStockDeducted flag).
        // COD orders already had stock deducted at order creation — this is a no-op for them.
        deductOrderStock(order);

        Order saved = orderRepository.save(order);

        // ---- Build email context SYNCHRONOUSLY inside the @Transactional boundary ----
        // All lazy associations (user, shippingAddress, items → product) are resolved HERE
        // before the @Async thread picks up the email task.
        OrderEmailContext emailCtx;
        try {
            emailCtx = buildEmailContext(saved);
        } catch (Exception e) {
            log.error("Could not build email context for order {} — emails skipped: {}",
                    saved.getOrderNumber(), e.getMessage());
            return mapToResponse(saved);
        }

        // Notify customer — order confirmed + payment verified
        if (emailCtx.getCustomerEmail() != null && !emailCtx.getCustomerEmail().isBlank()
                && !emailCtx.getCustomerEmail().startsWith("guest_")) {
            try {
                emailService.sendOrderConfirmationEmail(emailCtx.getCustomerEmail(), emailCtx);
            } catch (Exception e) {
                log.warn("Customer confirmation email failed for order {}: {}",
                        saved.getOrderNumber(), e.getMessage());
            }
        }

        // Notify admin — payment approved confirmation
        if (adminEmail != null && !adminEmail.isBlank()) {
            try {
                emailService.sendAdminPaymentApprovedAlert(adminEmail, emailCtx);
                // High-value VIP alert
                if (saved.getFinalAmount() != null
                        && saved.getFinalAmount().compareTo(BigDecimal.valueOf(25000)) >= 0) {
                    emailService.sendAdminVipOrderAlert(adminEmail, emailCtx);
                }
            } catch (Exception e) {
                log.warn("Admin payment approval alert failed for order {}: {}",
                        saved.getOrderNumber(), e.getMessage());
            }
        }

        log.info("Payment approved for Order {} [method={}] by admin",
                saved.getOrderNumber(), saved.getPaymentMethod());
        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + saved.getId() + "\",\"orderNumber\":\"" + saved.getOrderNumber() + "\"}");
        realtimeEventService.broadcast("STOCK_UPDATED", "{\"type\":\"STOCK_UPDATED\"}");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deductOrderStock(Order order) {
        if (order == null || Boolean.TRUE.equals(order.getIsStockDeducted())) {
            return; // Idempotent guard — prevent duplicate deduction
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            Product p = oi.getProduct();
            int qty = oi.getQuantity();
            if (oi.getVariant() != null) {
                ProductVariant variant = productVariantRepository.findByIdForUpdate(oi.getVariant().getId())
                        .orElse(oi.getVariant());
                variant.setStock(Math.max(0, variant.getStock() - qty));
                productVariantRepository.save(variant);
            } else if (p != null && p.getVariants() != null && !p.getVariants().isEmpty()) {
                ProductVariant firstV = productVariantRepository.findByIdForUpdate(p.getVariants().get(0).getId())
                        .orElse(p.getVariants().get(0));
                firstV.setStock(Math.max(0, firstV.getStock() - qty));
                productVariantRepository.save(firstV);
            }
        }
        order.setIsStockDeducted(true);
        orderRepository.save(order);
        log.info("Inventory successfully deducted for Order: {}", order.getOrderNumber());
    }

    @Override
    @Transactional
    public void restoreOrderStock(Order order) {
        if (order == null || !Boolean.TRUE.equals(order.getIsStockDeducted())) {
            return; // Stock was never deducted, no restore needed
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            Product p = oi.getProduct();
            int qty = oi.getQuantity();
            if (oi.getVariant() != null) {
                ProductVariant variant = productVariantRepository.findByIdForUpdate(oi.getVariant().getId())
                        .orElse(oi.getVariant());
                variant.setStock(variant.getStock() + qty);
                productVariantRepository.save(variant);
            } else if (p != null && p.getVariants() != null && !p.getVariants().isEmpty()) {
                ProductVariant firstV = productVariantRepository.findByIdForUpdate(p.getVariants().get(0).getId())
                        .orElse(p.getVariants().get(0));
                firstV.setStock(firstV.getStock() + qty);
                productVariantRepository.save(firstV);
            }
        }
        order.setIsStockDeducted(false);
        orderRepository.save(order);
        log.info("Inventory successfully restored for cancelled Order: {}", order.getOrderNumber());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public OrderResponse rejectManualPaymentAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Manual UPI payment verification failed / invalid proof");

        try {
            transactionRepository.findByOrderId(order.getId()).ifPresent(tx -> {
                tx.setStatus(PaymentStatus.FAILED);
                transactionRepository.save(tx);
            });
        } catch (Exception ignored) {}

        restoreOrderStock(order);

        Order saved = orderRepository.save(order);
        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + saved.getId() + "\",\"orderNumber\":\"" + saved.getOrderNumber() + "\"}");
        realtimeEventService.broadcast("STOCK_UPDATED", "{\"type\":\"STOCK_UPDATED\"}");
        try {
            emailService.sendOrderCancelledEmail(saved.getUser().getEmail(), buildEmailContext(saved));
        } catch (Exception e) {
            log.warn("Failed to send order rejection email: {}", e.getMessage());
        }
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboardStats() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countPendingOrders();
        long pendingPaymentVerification = orderRepository.countByStatus(OrderStatus.PAYMENT_PROOF_SUBMITTED);
        long deliveredOrders = orderRepository.countDeliveredOrders();
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long totalProducts = productRepository.count();
        long lowStockProducts = productRepository.countByStockLessThan(5);
        long totalCustomers = userRepository.count();

        List<OrderResponse> recent = orderRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, 5)
        ).getContent().stream().map(this::mapToResponse).toList();

        return AdminDashboardResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .pendingPaymentVerification(pendingPaymentVerification)
                .deliveredOrders(deliveredOrders)
                .totalRevenue(totalRevenue)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .totalCustomers(totalCustomers)
                .recentOrders(recent)
                .emailStats(emailUsageService.getEmailStats())
                .build();
    }

    @Override
    @Transactional
    public void processAbandonedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<Order> abandoned = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PLACED, cutoff);
        for (Order o : abandoned) {
            if (o.getPaymentStatus() == PaymentStatus.PENDING && o.getPaymentMethod() != PaymentMethod.COD) {
                o.setStatus(OrderStatus.CANCELLED);
                restoreOrderStock(o);
                orderRepository.save(o);
            }
        }
        log.info("Processed {} abandoned orders", abandoned.size());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public void deleteOrderAdmin(UUID orderId) {
        log.info("REST request by admin to permanently delete order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // 1. If stock was deducted and order is not already cancelled, safely restore stock
        if (Boolean.TRUE.equals(order.getIsStockDeducted()) && order.getStatus() != OrderStatus.CANCELLED) {
            restoreOrderStock(order);
        }

        // 2. Delete payment proof screenshot from storage if present
        try {
            transactionRepository.findByOrderId(orderId).ifPresent(tx -> {
                String proofUrl = tx.getPaymentProofUrl();
                if (proofUrl != null && !proofUrl.isBlank()) {
                    try {
                        fileStorageService.deleteFile(proofUrl);
                    } catch (Exception ex) {
                        log.warn("Failed to delete receipt file from storage for order {}: {}", orderId, ex.getMessage());
                    }
                }
            });
        } catch (Exception ignored) {}

        // 3. Clean up linked transaction and coupon usage records
        try {
            couponUsageRepository.deleteByOrderId(orderId);
        } catch (Exception ignored) {}

        try {
            transactionRepository.deleteByOrderId(orderId);
        } catch (Exception ignored) {}

        // 4. Delete the order (cascade deletes order items)
        orderRepository.delete(order);

        // 5. Broadcast real-time SSE updates to refresh admin views and stock
        realtimeEventService.broadcast("ORDER_UPDATED", "{\"type\":\"ORDER_UPDATED\",\"orderId\":\"" + orderId + "\"}");
        realtimeEventService.broadcast("STOCK_UPDATED", "{\"type\":\"STOCK_UPDATED\"}");
        log.info("Order {} successfully deleted by admin", order.getOrderNumber());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public void bulkDeleteOrdersAdmin(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return;
        log.info("REST request by admin to bulk delete {} orders", orderIds.size());
        for (UUID id : orderIds) {
            try {
                deleteOrderAdmin(id);
            } catch (Exception ex) {
                log.warn("Failed to delete order ID {} in bulk delete: {}", id, ex.getMessage());
            }
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream().map(oi -> {
            Product p = oi.getProduct();
            if (p == null && oi.getVariant() != null) {
                p = oi.getVariant().getProduct();
            }

            ProductDTO pDto = null;
            if (p != null && p.getId() != null) {
                try {
                    pDto = productService.getProductById(p.getId());
                } catch (Exception ignored) {}
            }

            UUID prodId = p != null ? p.getId() : null;
            String prodName = p != null ? p.getName() : "Handloom Saree";
            String prodSku = p != null ? p.getSku() : (oi.getVariant() != null ? oi.getVariant().getSku() : "SKU");
            String imgUrl = (p != null && p.getImageUrls() != null && !p.getImageUrls().isEmpty()) ? p.getImageUrls().get(0) : null;

            return OrderItemResponse.builder()
                    .id(oi.getId())
                    .productId(prodId)
                    .productName(prodName)
                    .productSku(prodSku)
                    .imageUrl(imgUrl)
                    .variantId(oi.getVariant() != null ? oi.getVariant().getId() : null)
                    .size(oi.getSize())
                    .color(oi.getColor())
                    .quantity(oi.getQuantity())
                    .price(oi.getPrice())
                    .totalPrice(oi.getTotalPrice())
                    .product(pDto)
                    .build();
        }).toList();

        AddressResponse addressResp = null;
        if (order.getShippingAddress() != null) {
            ShippingAddress sa = order.getShippingAddress();
            addressResp = AddressResponse.builder()
                    .id(sa.getId())
                    .fullName(sa.getFullName())
                    .phoneNumber(sa.getPhoneNumber())
                    .alternatePhone(sa.getAlternatePhone())
                    .addressLine1(sa.getAddressLine1())
                    .addressLine2(sa.getAddressLine2())
                    .city(sa.getCity())
                    .state(sa.getState())
                    .postalCode(sa.getPostalCode())
                    .country(sa.getCountry())
                    .addressType(sa.getAddressType())
                    .isDefault(sa.isDefault())
                    .createdAt(sa.getCreatedAt())
                    .build();
        }

        String utr = null;
        String proofUrl = null;
        try {
            Optional<com.pmrgsolution.features.payment.entity.Transaction> txOpt = transactionRepository.findByOrderId(order.getId());
            if (txOpt.isPresent()) {
                com.pmrgsolution.features.payment.entity.Transaction tx = txOpt.get();
                utr = tx.getUtrNumber();
                proofUrl = tx.getPaymentProofUrl();
            }
        } catch (Exception ignored) {}

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .userFullName(order.getUser().getFullName())
                .shippingAddress(addressResp)
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .codHandlingFee(order.getCodHandlingFee())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .status(order.getStatus())
                .trackingNumber(order.getTrackingNumber())
                .courierPartner(order.getCourierPartner())
                .trackingUrl(order.getTrackingUrl())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .couponCode(order.getCouponCode())
                .notes(order.getNotes())
                .cancellationReason(order.getCancellationReason())
                .utrNumber(utr)
                .paymentProofUrl(proofUrl)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}