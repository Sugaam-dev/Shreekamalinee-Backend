package com.PMRGSolution.RENAISSANCE.features.catalog.repository;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes; // <--- Use the new plural Enum
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    // Updated to use ContentTypes
    List<Document> findByContentType(ContentTypes type);

    /**
     * MAIN CATALOG FETCH
     */
    List<Document> findByCategoryIdOrderByDisplayOrderAsc(UUID categoryId);

    /**
     * FINAL SECURITY CHECK: Fixed query to use the new field name 'contentType'
     */
    @Query("SELECT d FROM Document d WHERE d.category.id = :categoryId " +
           "AND (d.contentType = 'SAMPLE_PDF' OR d.requiredTier <= :userTier)")
    List<Document> findAuthorizedDocuments(@Param("categoryId") UUID categoryId, 
                                            @Param("userTier") TierType userTier);
}