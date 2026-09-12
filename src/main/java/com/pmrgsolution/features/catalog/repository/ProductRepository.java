package com.pmrgsolution.features.catalog.repository;

import com.pmrgsolution.features.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
    long countByCategoryId(UUID categoryId);
    
    @Query("SELECT COUNT(DISTINCT p) FROM Product p JOIN p.variants v WHERE v.stockQuantity < :threshold")
    long countLowStock(@Param("threshold") int threshold);

    @Query("SELECT COUNT(DISTINCT p) FROM Product p JOIN p.variants v WHERE v.stockQuantity < :threshold")
    long countByStockLessThan(@Param("threshold") int threshold);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.category = null WHERE p.category.id = :categoryId")
    void unlinkCategoryFromProducts(@Param("categoryId") UUID categoryId);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId OR p.category.parentCategory.id = :categoryId")
    List<Product> findByCategoryIdRecursive(@Param("categoryId") UUID categoryId);
}