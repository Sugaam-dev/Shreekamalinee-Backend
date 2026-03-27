package com.PMRGSolution.RENAISSANCE.features.payment.repository;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.TierPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface TierPackageRepository extends JpaRepository<TierPackage, UUID> {
    
    // CRITICAL: Used during Checkout to get the exact price and duration
    Optional<TierPackage> findByCategoryIdAndTierTypeAndActiveTrue(UUID categoryId, TierType tierType);

    // HELPER: Used by Admin to see all active prices for a specific Exam Category
    List<TierPackage> findByCategoryIdAndActiveTrue(UUID categoryId);

    // HELPER: Used during setupTierPackage to check if we should update an existing price or create a new one
    Optional<TierPackage> findByCategoryIdAndTierType(UUID categoryId, TierType tierType);
}