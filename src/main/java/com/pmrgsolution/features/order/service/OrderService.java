package com.pmrgsolution.features.order.service;

import com.pmrgsolution.features.order.dto.AdminDashboardResponse;
import com.pmrgsolution.features.order.dto.CheckoutRequest;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.order.dto.OrderStatusUpdateRequest;
import com.pmrgsolution.features.order.dto.ShippingDetailsUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(UUID userId, CheckoutRequest request, String idempotencyKey);
    Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable);
    OrderResponse getOrderById(UUID userId, UUID orderId);
    OrderResponse getOrderByIdAdmin(UUID orderId);
    OrderResponse getOrderByOrderNumber(String orderNumber);
    /** SECURITY: Returns order only if the userId matches — prevents PII leaks */
    OrderResponse getOrderByOrderNumberForUser(String orderNumber, UUID userId);
    OrderResponse cancelOrderCustomer(UUID userId, UUID orderId);
    Page<OrderResponse> getAllOrdersAdmin(String status, Pageable pageable);
    /** Updates order lifecycle status only (CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED) */
    OrderResponse adminUpdateOrderStatus(UUID orderId, OrderStatusUpdateRequest request);
    /** Updates courier/tracking details only — separate from status changes */
    OrderResponse adminUpdateShippingDetails(UUID orderId, ShippingDetailsUpdateRequest request);
    OrderResponse approveManualPaymentAdmin(UUID orderId);
    OrderResponse rejectManualPaymentAdmin(UUID orderId);
    OrderResponse createAdminManualOrder(com.pmrgsolution.features.order.dto.AdminManualOrderRequest request);
    AdminDashboardResponse getAdminDashboardStats();
    void processAbandonedOrders();
    void deductOrderStock(com.pmrgsolution.features.order.entity.Order order);
    void restoreOrderStock(com.pmrgsolution.features.order.entity.Order order);
    void deleteOrderAdmin(UUID orderId);
    void bulkDeleteOrdersAdmin(List<UUID> orderIds);
}