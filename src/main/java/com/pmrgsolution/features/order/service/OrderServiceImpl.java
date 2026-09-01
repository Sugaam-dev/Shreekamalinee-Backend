package com.pmrgsolution.features.order.service;

import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.features.address.dto.AddressResponse;
import com.pmrgsolution.features.address.entity.ShippingAddress;
import com.pmrgsolution.features.address.repository.ShippingAddressRepository;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.auth.service.EmailService;
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
import com.pmrgsolution.features.order.entity.Order;
import com.pmrgsolution.features.order.entity.OrderItem;
import com.pmrgsolution.features.order.repository.OrderItemRepository;
import com.pmrgsolution.features.order.repository.OrderRepository;
import com.pmrgsolution.features.settings.dto.StoreSettingsResponse;
import com.pmrgsolution.features.settings.service.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    private final com.pmrgsolution.features.payment.repository.TransactionRepository transactionRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Value("${app.admin.notification-email:${app.admin.email:admin@shreekamalinee.com}}")
    private String adminEmail;

    /** IDEMPOTENCY: Redis-backed key store with 24h TTL — survives restarts and works across multiple instances */
    private static final String IDEMPOTENCY_KEY_PREFIX = "idem:order:";
    private static final java.time.Duration IDEMPOTENCY_TTL = java.time.Duration.ofHours(24);

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public OrderResponse createOrder(UUID userId, CheckoutRequest request, String idempotencyKey) {
        // IDEMPOTENCY CHECK: Use Redis so this works across restarts and multiple application instances
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Boolean alreadyProcessed = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(alreadyProcessed)) {
                log.info("Idempotency key already processed, rejecting duplicate: {}", idempotencyKey);
                throw new BusinessException("This order has already been placed. Please check your orders page.", org.springframework.http.HttpStatus.CONFLICT);
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
                        .collect(Collectors.toList());
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
        if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            codHandlingFee = settings.getCodHandlingFee() != null ? settings.getCodHandlingFee() : BigDecimal.valueOf(99.00);
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
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .paymentStatus("PENDING")
                .status("PENDING")
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

            // RACE CONDITION FIX: Re-fetch variant with PESSIMISTIC_WRITE lock to prevent overselling
            if (directVariant != null) {
                ProductVariant lockedVariant = productVariantRepository.findByIdForUpdate(directVariant.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
                if (lockedVariant.getStock() < directQuantity) {
                    throw new BusinessException("Insufficient stock for product: " + directProduct.getName(), org.springframework.http.HttpStatus.CONFLICT);
                }
                lockedVariant.setStock(Math.max(0, lockedVariant.getStock() - directQuantity));
                productVariantRepository.save(lockedVariant);
            } else if (directProduct.getVariants() != null && !directProduct.getVariants().isEmpty()) {
                ProductVariant firstV = directProduct.getVariants().get(0);
                firstV.setStock(Math.max(0, firstV.getStock() - directQuantity));
                productVariantRepository.save(firstV);
            }
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

                if (ci.getVariant() != null) {
                    ProductVariant variant = ci.getVariant();
                    variant.setStock(Math.max(0, variant.getStock() - ci.getQuantity()));
                    productVariantRepository.save(variant);
                } else if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                    ProductVariant firstV = p.getVariants().get(0);
                    firstV.setStock(Math.max(0, firstV.getStock() - ci.getQuantity()));
                    productVariantRepository.save(firstV);
                }
            }

            // Only delete the items that were purchased from the cart!
            cartItemRepository.deleteAll(purchasedCartItems);
        }

        // Only record coupon usage if payment is confirmed (e.g. COD or immediate PAID)
        // For Manual UPI & Razorpay, coupon usage is recorded upon payment receipt upload / payment verification
        if (appliedCoupon != null && ("COD".equalsIgnoreCase(savedOrder.getPaymentMethod()) || "PAID".equalsIgnoreCase(savedOrder.getPaymentStatus()))) {
            appliedCoupon.setTimesUsed(appliedCoupon.getTimesUsed() + 1);
            couponRepository.save(appliedCoupon);

            CouponUsage usage = CouponUsage.builder()
                    .coupon(appliedCoupon)
                    .user(user)
                    .orderId(savedOrder.getId())
                    .build();
            couponUsageRepository.save(usage);
        }

        // Customer Confirmation Email (Only for confirmed COD or already PAID orders)
        if ("COD".equalsIgnoreCase(savedOrder.getPaymentMethod()) || "PAID".equalsIgnoreCase(savedOrder.getPaymentStatus())) {
            try {
                emailService.sendOrderConfirmationEmail(user.getEmail(), savedOrder);
            } catch (Exception e) {
                log.warn("Failed to send order confirmation email: {}", e.getMessage());
            }
        }

        // Admin Notification Email Alert (Only for confirmed COD or already PAID orders — NOT for unverified pending UPI orders)
        if ("COD".equalsIgnoreCase(savedOrder.getPaymentMethod()) || "PAID".equalsIgnoreCase(savedOrder.getPaymentStatus())) {
            try {
                if (adminEmail != null && !adminEmail.isBlank()) {
                    emailService.sendAdminNewOrderAlert(adminEmail, savedOrder);
                    if (savedOrder.getFinalAmount() != null && savedOrder.getFinalAmount().compareTo(BigDecimal.valueOf(25000)) >= 0) {
                        emailService.sendAdminVipOrderAlert(adminEmail, savedOrder);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to dispatch admin new order email alert: {}", e.getMessage());
            }
        }

        OrderResponse response = mapToResponse(savedOrder);
        // Record idempotency key in Redis with TTL so future duplicates are rejected
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            redisTemplate.opsForValue().set(
                IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                savedOrder.getId().toString(),
                IDEMPOTENCY_TTL
            );
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                            .role(com.pmrgsolution.Constant.Role.USER)
                            .enabled(true)
                            .accountNonLocked(true)
                            .provider(com.pmrgsolution.Constant.AuthProvider.LOCAL)
                            .password(UUID.randomUUID().toString())
                            .build();
                    return userRepository.save(newUser);
                });

        // 2. Create Shipping Address
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
                .addressType("SHIPPING")
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
            variant.setStock(variant.getStock() - qty);
            productVariantRepository.save(variant);
        } else if (product.getStock() < qty) {
            throw new BusinessException("Insufficient stock for product: " + product.getName(), HttpStatus.CONFLICT);
        } else {
            product.setStock(product.getStock() - qty);
            productRepository.save(product);
        }

        BigDecimal unitPrice = product.getOfferPrice() != null ? product.getOfferPrice() : product.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

        // 4. Coupon Validation & Consumption
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponValidationResponse val = couponService.validateCoupon(request.getCouponCode().trim(), subtotal, user.getId());
            if (val.isValid()) {
                discountAmount = val.getCalculatedDiscount();
                appliedCoupon = couponRepository.findByCodeIgnoreCase(request.getCouponCode().trim()).orElse(null);
            } else {
                log.warn("Coupon validation note for manual order: {}", val.getMessage());
            }
        }

        BigDecimal finalAmount = subtotal.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            String randomPart = String.format("%04d", (int)(Math.random() * 9000 + 1000));
            orderNumber = "SK-" + datePart + "-" + randomPart;
        } while (orderRepository.findByOrderNumber(orderNumber).isPresent());

        String payStatus = "PAID".equalsIgnoreCase(request.getPaymentStatus()) ? "PAID" : "PENDING";
        String payMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "WHATSAPP_UPI";
        String orderStatus = "PAID".equals(payStatus) ? "PROCESSING" : "PLACED";

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(address)
                .totalAmount(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(BigDecimal.ZERO)
                .codHandlingFee(BigDecimal.ZERO)
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
                emailService.sendOrderConfirmationEmail(cleanEmail, order);
            } catch (Exception e) {
                log.warn("Failed to dispatch manual order confirmation email to {}: {}", cleanEmail, e.getMessage());
            }
        }

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

        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException("Order cannot be cancelled in status: " + order.getStatus(), HttpStatus.BAD_REQUEST);
        }

        order.setStatus("CANCELLED");
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            order.setPaymentStatus("REFUND_PENDING");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            Product p = oi.getProduct();
            if (oi.getVariant() != null) {
                // Has an explicit variant — restore its stock
                ProductVariant variant = oi.getVariant();
                variant.setStock(variant.getStock() + oi.getQuantity());
                productVariantRepository.save(variant);
            } else if (p != null) {
                // BUG FIX: No variant attached. Previously this branch restored stock to variants.get(0)
                // which is WRONG — it would restore the wrong item's inventory.
                // If the product has variants, we cannot safely restore (we don't know which variant was ordered).
                // If the product has no variants (uses product-level stock), restore product stock.
                if (p.getVariants() == null || p.getVariants().isEmpty()) {
                    p.setStock(p.getStock() + oi.getQuantity());
                    productRepository.save(p);
                } else {
                    log.warn("Cannot safely restore stock for order item {} — no variant attached but product has variants.", oi.getId());
                }
            }
        }

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersAdmin(String status, Pageable pageable) {
        Page<Order> page;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            page = orderRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
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

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String newStatus = request.getStatus().toUpperCase();
            if ("CANCELLED".equals(newStatus) && !"CANCELLED".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                if (request.getCancellationReason() != null && !request.getCancellationReason().isBlank()) {
                    order.setCancellationReason(request.getCancellationReason().trim());
                } else if (order.getCancellationReason() == null) {
                    order.setCancellationReason("Cancelled by Store Administrator");
                }
                if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                    order.setPaymentStatus("REFUND_PENDING");
                }
                // Replenish inventory
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                for (OrderItem oi : items) {
                    Product p = oi.getProduct();
                    if (oi.getVariant() != null) {
                        ProductVariant variant = oi.getVariant();
                        variant.setStock(variant.getStock() + oi.getQuantity());
                        productVariantRepository.save(variant);
                    } else if (p != null && p.getVariants() != null && !p.getVariants().isEmpty()) {
                        ProductVariant firstV = p.getVariants().get(0);
                        firstV.setStock(firstV.getStock() + oi.getQuantity());
                        productVariantRepository.save(firstV);
                    }
                }
                try {
                    emailService.sendOrderCancelledEmail(order.getUser().getEmail(), order, order.getCancellationReason());
                    if (adminEmail != null && !adminEmail.isBlank()) {
                        emailService.sendAdminOrderCancelledAlert(adminEmail, order);
                    }
                } catch (Exception e) {
                    log.warn("Failed to send order cancellation email: {}", e.getMessage());
                }
            } else {
                order.setStatus(newStatus);
                if ("SHIPPED".equals(newStatus)) {
                    try {
                        emailService.sendOrderShippedEmail(order.getUser().getEmail(), order);
                    } catch (Exception e) {
                        log.warn("Failed to send order shipped email: {}", e.getMessage());
                    }
                } else if ("DELIVERED".equals(newStatus)) {
                    try {
                        emailService.sendOrderDeliveredEmail(order.getUser().getEmail(), order);
                    } catch (Exception e) {
                        log.warn("Failed to send order delivered email: {}", e.getMessage());
                    }
                }
            }
        }

        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isBlank()) {
            order.setPaymentStatus(request.getPaymentStatus().toUpperCase());
        }
        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(request.getTrackingNumber().trim());
        }
        if (request.getCourierPartner() != null) {
            order.setCourierPartner(request.getCourierPartner().trim());
        }
        if (request.getTrackingUrl() != null) {
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
                log.warn("Could not parse estimatedDeliveryDate: {}", request.getEstimatedDeliveryDate());
            }
        }
        if (request.getCancellationReason() != null && !request.getCancellationReason().isBlank()) {
            order.setCancellationReason(request.getCancellationReason().trim());
        }

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public OrderResponse approveManualPaymentAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        order.setPaymentStatus("PAID");
        if ("PENDING".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("PROCESSING");
        }

        try {
            transactionRepository.findByOrderId(order.getId()).ifPresent(tx -> {
                tx.setStatus("SUCCESS");
                transactionRepository.save(tx);
            });
        } catch (Exception ignored) {}

        Order saved = orderRepository.save(order);
        try {
            emailService.sendOrderConfirmationEmail(order.getUser().getEmail(), order);
        } catch (Exception e) {
            log.warn("Failed to send order confirmation email upon payment approval: {}", e.getMessage());
        }
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog", "categories"}, allEntries = true)
    public OrderResponse rejectManualPaymentAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        order.setPaymentStatus("FAILED");
        order.setStatus("CANCELLED");
        order.setCancellationReason("Manual UPI payment verification failed / invalid proof");

        try {
            transactionRepository.findByOrderId(order.getId()).ifPresent(tx -> {
                tx.setStatus("FAILED");
                transactionRepository.save(tx);
            });
        } catch (Exception ignored) {}

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            Product p = oi.getProduct();
            if (oi.getVariant() != null) {
                ProductVariant variant = oi.getVariant();
                variant.setStock(variant.getStock() + oi.getQuantity());
                productVariantRepository.save(variant);
            } else if (p != null && p.getVariants() != null && !p.getVariants().isEmpty()) {
                ProductVariant firstV = p.getVariants().get(0);
                firstV.setStock(firstV.getStock() + oi.getQuantity());
                productVariantRepository.save(firstV);
            }
        }

        Order saved = orderRepository.save(order);
        try {
            emailService.sendOrderCancelledEmail(order.getUser().getEmail(), order, order.getCancellationReason());
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
        long deliveredOrders = orderRepository.countDeliveredOrders();
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long totalProducts = productRepository.count();
        long lowStockProducts = productRepository.countByStockLessThan(5);
        long totalCustomers = userRepository.count();

        List<OrderResponse> recent = orderRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, 5)
        ).getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

        return AdminDashboardResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .deliveredOrders(deliveredOrders)
                .totalRevenue(totalRevenue)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .totalCustomers(totalCustomers)
                .recentOrders(recent)
                .build();
    }

    @Override
    @Transactional
    public void processAbandonedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<Order> abandoned = orderRepository.findByStatusAndCreatedAtBefore("PENDING", cutoff);
        for (Order o : abandoned) {
            if ("PENDING".equalsIgnoreCase(o.getPaymentStatus()) && !"COD".equalsIgnoreCase(o.getPaymentMethod())) {
                o.setStatus("CANCELLED");
                orderRepository.save(o);
                List<OrderItem> items = orderItemRepository.findByOrderId(o.getId());
                for (OrderItem oi : items) {
                    if (oi.getVariant() != null) {
                        // BUG FIX: was using setStockQuantity() — inconsistent with rest of codebase
                        // which uses setStock(). Both set stockQuantity field but setStock() is the
                        // canonical setter defined in ProductVariant entity.
                        ProductVariant v = oi.getVariant();
                        v.setStock(v.getStock() + oi.getQuantity());
                        productVariantRepository.save(v);
                    }
                    // BUG FIX: removed the previous productRepository.save(p) — it saved the
                    // product entity with NO changes made to it, which was a needless DB write.
                }
            }
        }
        log.info("Processed {} abandoned orders", abandoned.size());
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
        }).collect(Collectors.toList());

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