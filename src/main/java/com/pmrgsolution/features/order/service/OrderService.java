package com.pmrgsolution.features.order.service;

import com.pmrgsolution.features.order.dto.AdminDashboardResponse;
import com.pmrgsolution.features.order.dto.CheckoutRequest;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.order.dto.OrderStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(UUID userId, CheckoutRequest request, String idempotencyKey);
    List<OrderResponse> getUserOrders(UUID userId);
    OrderResponse getOrderById(UUID userId, UUID orderId);
    OrderResponse getOrderByIdAdmin(UUID orderId);
    OrderResponse getOrderByOrderNumber(String orderNumber);
    /** SECURITY: Returns order only if the userId matches — prevents PII leaks */
    OrderResponse getOrderByOrderNumberForUser(String orderNumber, UUID userId);
    OrderResponse cancelOrderCustomer(UUID userId, UUID orderId);
    Page<OrderResponse> getAllOrdersAdmin(String status, Pageable pageable);
    OrderResponse adminUpdateOrderStatus(UUID orderId, OrderStatusUpdateRequest request);
    OrderResponse approveManualPaymentAdmin(UUID orderId);
    OrderResponse rejectManualPaymentAdmin(UUID orderId);
    OrderResponse createAdminManualOrder(com.pmrgsolution.features.order.dto.AdminManualOrderRequest request);
    AdminDashboardResponse getAdminDashboardStats();
    void processAbandonedOrders();
}