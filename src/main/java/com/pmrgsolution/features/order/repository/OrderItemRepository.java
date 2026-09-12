package com.pmrgsolution.features.order.repository;

import com.pmrgsolution.features.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.product = null, oi.variant = null WHERE oi.product.id = :productId")
    void nullifyProductReferences(@Param("productId") UUID productId);
}