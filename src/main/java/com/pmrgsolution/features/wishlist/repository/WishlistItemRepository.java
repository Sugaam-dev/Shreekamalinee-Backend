package com.pmrgsolution.features.wishlist.repository;

import com.pmrgsolution.features.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
    
    List<WishlistItem> findByUserId(UUID userId);

    @Query("SELECT w FROM WishlistItem w JOIN FETCH w.product WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<WishlistItem> findByUserIdWithProduct(@Param("userId") UUID userId);
    
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM WishlistItem w WHERE w.user.id = :userId AND w.product.id = :productId")
    void deleteByUserIdAndProductId(@Param("userId") UUID userId, @Param("productId") UUID productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WishlistItem w WHERE w.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WishlistItem w WHERE w.product.id = :productId")
    void deleteByProductId(@Param("productId") UUID productId);
}

