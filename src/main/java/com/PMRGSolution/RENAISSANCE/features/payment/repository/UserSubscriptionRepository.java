package com.PMRGSolution.RENAISSANCE.features.payment.repository;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    /**
     * OPTIMIZED SECURITY CHECK: Uses JOIN FETCH to get Category details immediately.
     * Prevents extra queries when checking access in ExamServiceImpl.
     */
    @Query("SELECT s FROM UserSubscription s " +
           "JOIN FETCH s.category " + 
           "WHERE s.user.email = :email " +
           "AND s.category.id = :categoryId " +
           "AND s.active = true " +
           "AND s.expiryDate > :now")
    Optional<UserSubscription> findActiveSubscription(
            @Param("email") String email, 
            @Param("categoryId") UUID categoryId, 
            @Param("now") LocalDateTime now);

    /**
     * OPTIMIZED DASHBOARD HYDRATION: JOIN FETCH category and user.
     * This is the fix for the N+1 problem in your ProfileServiceImpl.
     */
    @Query("SELECT s FROM UserSubscription s " +
           "JOIN FETCH s.category " +
           "JOIN FETCH s.user " +
           "WHERE s.user.email = :email " +
           "AND s.active = true " +
           "AND s.expiryDate >= :thresholdDate")
    List<UserSubscription> findAllActiveSubscriptions(
            @Param("email") String email, 
            @Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * STEP 3 PREVIEW: High-speed ID retrieval.
     * Returns only the UUIDs of active categories. Very light on RAM.
     */
    @Query("SELECT s.category.id FROM UserSubscription s " +
           "WHERE s.user.email = :email " +
           "AND s.active = true " +
           "AND s.expiryDate > :now")
    Set<UUID> findActiveCategoryIds(
            @Param("email") String email, 
            @Param("now") LocalDateTime now);

    /**
     * HIGH-SPEED CHECK: Keep as is (COUNT is already optimized).
     */
    @Query("SELECT COUNT(s) > 0 FROM UserSubscription s " +
           "WHERE s.user.email = :email " +
           "AND s.category.id = :categoryId " +
           "AND s.active = true " +
           "AND s.expiryDate > :now")
    boolean existsActiveSubscription(
            @Param("email") String email, 
            @Param("categoryId") UUID categoryId, 
            @Param("now") LocalDateTime now);
    
    
    /**
     * FIXED RANK CHECK: Fetches the Enum itself. 
     * We extract the rank in the Service layer to avoid JPQL Path errors.
     */
    @Query("SELECT s.tier FROM UserSubscription s " +
           "WHERE s.user.email = :email " +
           "AND s.category.id = :categoryId " +
           "AND s.active = true " +
           "AND s.expiryDate > :now")
    Optional<TierType> findActiveTier(
            @Param("email") String email, 
            @Param("categoryId") UUID categoryId, 
            @Param("now") LocalDateTime now);
}