package com.PMRGSolution.RENAISSANCE.features.catalog.repository;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PackageRepository extends JpaRepository<Package, UUID> {

    /**
     * THE DISCOVERY BRIDGE:
     * Returns all packages (Single or Combo) that include this Category.
     * Use this when the user clicks a locked exam and needs to see "Buy" options.
     */
    List<Package> findByAccessibleCategoriesId(UUID categoryId);

    /**
     * TIER-CATEGORY MATCH:
     * Find the package defining limits for a specific Tier and Category.
     */
    @Query("SELECT p FROM Package p JOIN p.accessibleCategories c WHERE p.tierType = :tier AND c.id = :catId")
    Optional<Package> findByTierAndCategoryId(@Param("tier") TierType tier, @Param("catId") UUID catId);

    /**
     * SECURE PAYMENT SYNC:
     * Finds packages that grant access to a specific category by Tier.
     */
    @Query("SELECT p FROM Package p JOIN p.accessibleCategories c WHERE p.tierType = :tier AND c = :category")
    List<Package> findByTierTypeAndAccessibleCategoriesContaining(
            @Param("tier") TierType tier, 
            @Param("category") ExamCategory category
    );
}