package com.PMRGSolution.RENAISSANCE.features.payment.repository;

import com.PMRGSolution.RENAISSANCE.features.payment.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    /**
     * CORE SECURITY: Finds exactly one active sub for a specific category.
     * Used by ExamSessionService and CatalogService gatekeepers.
     */
    @Query("SELECT s FROM UserSubscription s WHERE s.user.email = :email " +
           "AND s.category.id = :categoryId " +
           "AND s.active = true " +
           "AND s.expiryDate > :now")
    Optional<UserSubscription> findActiveSubscription(
            @Param("email") String email, 
            @Param("categoryId") UUID categoryId, 
            @Param("now") LocalDateTime now);

    /**
     * DASHBOARD HYDRATION: Finds all subscriptions for the profile view.
     * Note: 'now' here can be set to LocalDateTime.now().minusDays(2) 
     * by the Service to show recently expired plans.
     */
    @Query("SELECT s FROM UserSubscription s WHERE s.user.email = :email " +
           "AND s.active = true " +
           "AND s.expiryDate >= :thresholdDate")
    List<UserSubscription> findAllActiveSubscriptions(
            @Param("email") String email, 
            @Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * HIGH-SPEED CHECK: Returns boolean without loading full objects.
     */
    @Query("SELECT COUNT(s) > 0 FROM UserSubscription s WHERE s.user.email = :email " +
           "AND s.category.id = :categoryId " +
           "AND s.active = true " +
           "AND s.expiryDate > :now")
    boolean existsActiveSubscription(
            @Param("email") String email, 
            @Param("categoryId") UUID categoryId, 
            @Param("now") LocalDateTime now);
}