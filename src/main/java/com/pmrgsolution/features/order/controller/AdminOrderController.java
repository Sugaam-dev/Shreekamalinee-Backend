package com.pmrgsolution.features.order.controller;

import com.pmrgsolution.features.order.dto.AdminDashboardResponse;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.order.dto.OrderStatusUpdateRequest;
import com.pmrgsolution.features.order.dto.ShippingDetailsUpdateRequest;
import com.pmrgsolution.features.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrdersAdmin(status, pageable));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderByIdAdmin(orderId));
    }

    /**
     * Updates order lifecycle status only.
     * Valid values: CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
     * For courier/tracking details use PUT /{orderId}/shipping
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.adminUpdateOrderStatus(orderId, request));
    }

    /**
     * Updates courier/tracking details only — does NOT change order status.
     * Call this after marking status as SHIPPED to add tracking number, courier partner, etc.
     * If tracking is added to an already SHIPPED order, the customer receives an updated shipping email.
     */
    @PutMapping("/{orderId}/shipping")
    public ResponseEntity<OrderResponse> updateShippingDetails(
            @PathVariable UUID orderId,
            @Valid @RequestBody ShippingDetailsUpdateRequest request) {
        return ResponseEntity.ok(orderService.adminUpdateShippingDetails(orderId, request));
    }

    /**
     * Approves a manual UPI/bank payment proof submitted by the customer.
     * Sets paymentStatus=PAID, order status=CONFIRMED, sends confirmation email to customer,
     * and sends new order alert to admin.
     */
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<OrderResponse> approvePayment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.approveManualPaymentAdmin(orderId));
    }

    /**
     * Rejects a manual payment proof — order is CANCELLED, customer is notified.
     */
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<OrderResponse> rejectPayment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.rejectManualPaymentAdmin(orderId));
    }

    @PostMapping("/manual")
    public ResponseEntity<OrderResponse> createManualOrder(
            @Valid @RequestBody com.pmrgsolution.features.order.dto.AdminManualOrderRequest request) {
        return ResponseEntity.ok(orderService.createAdminManualOrder(request));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrderAdmin(orderId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDeleteOrders(@RequestBody java.util.List<UUID> orderIds) {
        orderService.bulkDeleteOrdersAdmin(orderIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(orderService.getAdminDashboardStats());
    }
}