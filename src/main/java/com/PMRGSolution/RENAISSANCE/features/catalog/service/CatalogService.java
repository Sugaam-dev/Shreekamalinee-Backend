package com.PMRGSolution.RENAISSANCE.features.catalog.service;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes;
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.catalog.dto.*;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface CatalogService {
    List<CategoryResponse> getExamsForUser(UUID userId);
    List<DocumentResponse> getDocumentsByExam(UUID categoryId, UUID userId);
    List<DocumentResponse> getUniversalFreeSamples();

    Package createPackage(PackageRequest details);
    Package updatePackage(UUID id, PackageRequest details);
    List<Package> getAllPackages();
    List<PackageResponse> getPackagesByCategoryId(UUID categoryId);

    DocumentResponse uploadDocument(String title, UUID catId, ContentTypes type, 
                                  TierType requiredTier, Integer displayOrder, 
                                  MultipartFile file) throws IOException;
    
    byte[] getWatermarkedStream(UUID documentId, UUID userId) throws IOException;
    String generateSecureViewLink(UUID documentId, UUID userId);
    CategoryResponse saveCategory(ExamCategory category);
    void deleteDocument(UUID documentId);
    
    DocumentResponse updateDocumentMetadata(UUID documentId, String title, ContentTypes type, 
                                            TierType requiredTier, Integer displayOrder);
    
    void verifySecureLink(UUID docId, UUID userId, String token, long expires);
}