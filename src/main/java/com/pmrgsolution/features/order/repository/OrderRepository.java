package com.pmrgsolution.features.order.repository;

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
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status != 'CANCELLED'")
    long countActiveOrders();

    @Query("SELECT COALESCE(SUM(COALESCE(o.finalAmount, o.totalAmount)), 0) FROM Order o WHERE o.paymentStatus = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING'")
    long countPendingOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED'")
    long countDeliveredOrders();

    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);

    long countByStatus(String status);
}