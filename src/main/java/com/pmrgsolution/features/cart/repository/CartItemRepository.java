package com.pmrgsolution.features.cart.repository;

import com.pmrgsolution.features.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);
    Optional<CartItem> findByCartIdAndProductVariantId(UUID cartId, UUID productVariantId);
    void deleteByCartId(UUID cartId);
}