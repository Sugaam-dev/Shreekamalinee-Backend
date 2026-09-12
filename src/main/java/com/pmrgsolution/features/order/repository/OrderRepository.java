package com.pmrgsolution.features.order.repository;

import com.pmrgsolution.constant.OrderStatus;
import com.pmrgsolution.features.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status != com.pmrgsolution.constant.OrderStatus.CANCELLED")
    long countActiveOrders();

    @Query("SELECT COALESCE(SUM(COALESCE(o.finalAmount, o.totalAmount)), 0) FROM Order o WHERE o.paymentStatus = com.pmrgsolution.constant.PaymentStatus.PAID")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = com.pmrgsolution.constant.OrderStatus.PLACED OR o.status = com.pmrgsolution.constant.OrderStatus.PAYMENT_PROOF_SUBMITTED")
    long countPendingOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = com.pmrgsolution.constant.OrderStatus.DELIVERED")
    long countDeliveredOrders();

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime threshold);

    long countByStatus(OrderStatus status);
}