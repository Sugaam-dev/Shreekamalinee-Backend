package com.pmrgsolution.features.catalog.repository;

import com.pmrgsolution.features.catalog.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user WHERE r.product.id = :productId ORDER BY r.createdAt DESC")
    List<Review> findByProductIdWithUser(@Param("productId") UUID productId);

    Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRating(@Param("productId") UUID productId);

    Long countByProductId(UUID productId);
}
