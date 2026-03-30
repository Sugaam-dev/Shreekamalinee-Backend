package com.PMRGSolution.RENAISSANCE.features.catalog.service;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.*;
import org.springframework.transaction.annotation.Transactional;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes;
import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import com.PMRGSolution.RENAISSANCE.Exception.BusinessException;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.UserRepository;
import com.PMRGSolution.RENAISSANCE.features.catalog.dto.*;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Document;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package;
import com.PMRGSolution.RENAISSANCE.features.catalog.repository.DocumentRepository;
import com.PMRGSolution.RENAISSANCE.features.catalog.repository.PackageRepository;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.Exam;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import com.PMRGSolution.RENAISSANCE.features.exam.repository.ExamCategoryRepository;
import com.PMRGSolution.RENAISSANCE.features.exam.repository.ExamRepository;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.UserSubscription;
import com.PMRGSolution.RENAISSANCE.features.payment.repository.UserSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final DocumentRepository documentRepository;
    private final PackageRepository packageRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ExamCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;

    @Value("${renaissance.hmac-secret}")
    private String hmacSecret;

    @Override
    public List<CategoryResponse> getExamsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        List<ExamCategory> allCategories = categoryRepository.findAll();
        
        // FETCH: Uses our optimized JOIN FETCH query
        List<UserSubscription> activeSubs = userSubscriptionRepository
                .findAllActiveSubscriptions(user.getEmail(), LocalDateTime.now());

        return allCategories.stream().map(cat -> {
            // OPTIMIZATION: Match category in memory
            Optional<UserSubscription> subMatch = activeSubs.stream()
                    .filter(sub -> sub.getCategory().getId().equals(cat.getId()))
                    .findFirst();

            boolean isSubscribed = subMatch.isPresent();
            Long daysLeft = isSubscribed ? 
                Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), subMatch.get().getExpiryDate())) 
                : null;

            return CategoryResponse.builder()
                    .id(cat.getId())
                    .displayName(cat.getDisplayName())
                    .description(cat.getDescription())
                    .isSubscribed(isSubscribed)
                    .daysRemaining(daysLeft)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByExam(UUID categoryId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        // 1. Fetch the Tier Enum itself (The Repository Fix)
        TierType userTier = userSubscriptionRepository.findActiveTier(
                user.getEmail(), categoryId, LocalDateTime.now())
                .orElse(TierType.UNIVERSAL_FREE);

        // 2. Get the rank from the Java Object (Safe and fast)
        int userTierRank = userTier.getRank();

        // 3. Map Static Documents (PDFs)
        List<DocumentResponse> content = documentRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId)
                .stream()
                .map(doc -> {
                    DocumentResponse resp = mapToDocDto(doc);
                    boolean isLocked = (userTierRank < doc.getRequiredTier().getRank()) 
                                       && (doc.getContentType() != ContentTypes.SAMPLE_PDF);
                    resp.setLocked(isLocked);
                    resp.setInteractive(false); 
                    return resp;
                }).collect(Collectors.toCollection(ArrayList::new));

        // 4. Map Interactive Exams (Mock Tests, Quizzes)
        List<DocumentResponse> exams = examRepository.findByCategoryId(categoryId)
                .stream()
                .filter(Exam::isPublished)
                .map(exam -> DocumentResponse.builder()
                        .id(exam.getId())
                        .title(exam.getTitle())
                        .documentType(exam.getContentType().name()) 
                        .categoryName(exam.getCategory().getDisplayName())
                        .requiredTier(exam.getRequiredTier().name())
                        .displayOrder(exam.getDisplayOrder() != null ? exam.getDisplayOrder() : 0)
                        .createdAt(exam.getCreatedAt())
                        .locked(userTierRank < exam.getRequiredTier().getRank()) // Using userTierRank
                        .interactive(true) 
                        .duration(exam.getDurationMinutes()) 
                        .build())
                .collect(Collectors.toList());

        content.addAll(exams);
        content.sort(Comparator.comparing(DocumentResponse::getDisplayOrder, 
                     Comparator.nullsLast(Comparator.naturalOrder()))
                     .thenComparing(DocumentResponse::getCreatedAt, 
                     Comparator.nullsLast(Comparator.reverseOrder())));
        
        return content;
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(String title, UUID catId, ContentTypes type, TierType tier, Integer order, MultipartFile file) throws IOException {
        ExamCategory cat = categoryRepository.findById(catId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        Document doc = Document.builder()
                .title(title)
                .category(cat)
                .contentType(type)
                .requiredTier(tier != null ? tier : TierType.UNIVERSAL_FREE)
                .displayOrder(order != null ? order : 0)
                .content(file.getBytes())
                .build();
        return mapToDocDto(documentRepository.save(doc));
    }

    @Override
    @Transactional
    public Package createPackage(PackageRequest details) {
        List<ExamCategory> categories = categoryRepository.findAllById(details.getCategoryIds());
        if (categories.isEmpty()) throw new BusinessException("Package must be linked to a Category");
        Package pkg = Package.builder()
                .name(details.getName())
                .priceAmount(details.getPriceAmount())
                .tierType(TierType.valueOf(details.getTierType()))
                .durationInMonths(details.getDurationInMonths())
                .mockTestLimit(details.getMockTestLimit() != null ? details.getMockTestLimit() : 0)
                .accessibleCategories(categories)
                .build();
        return packageRepository.save(pkg);
    }

    @Override
    public byte[] getWatermarkedStream(UUID documentId, UUID userId) throws IOException {
        Document doc = documentRepository.findById(documentId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        if (!doc.isProtectedFile()) return doc.getContent();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(doc.getContent());
            PdfStamper stamper = new PdfStamper(reader, out);
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);
            String watermark = "Renaissance | " + user.getEmail();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                PdfContentByte over = stamper.getOverContent(i);
                over.saveState();
                PdfGState gs = new PdfGState(); gs.setFillOpacity(0.2f); over.setGState(gs);
                over.beginText(); over.setFontAndSize(bf, 30); over.setRGBColorFill(150, 150, 150);
                over.showTextAligned(Element.ALIGN_CENTER, watermark, 297, 421, 45);
                over.endText(); over.restoreState();
            }
            stamper.close(); reader.close();
            return out.toByteArray();
        } catch (DocumentException e) { throw new BusinessException("Watermark failed", HttpStatus.INTERNAL_SERVER_ERROR); }
    }

    @Override
    public String generateSecureViewLink(UUID docId, UUID userId) {
        Document doc = documentRepository.findById(docId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        Optional<UserSubscription> sub = userSubscriptionRepository.findActiveSubscription(user.getEmail(), doc.getCategory().getId(), LocalDateTime.now());
        TierType userTier = sub.map(UserSubscription::getTier).orElse(TierType.UNIVERSAL_FREE);

        if (doc.getContentType() != ContentTypes.SAMPLE_PDF && userTier.getRank() < doc.getRequiredTier().getRank()) {
            throw new BusinessException("Subscription required", HttpStatus.PAYMENT_REQUIRED);
        }
        long expiry = System.currentTimeMillis() + 300000; // 5 minute validity
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(hmacSecret.getBytes(), "HmacSHA256"));
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((docId + ":" + userId + ":" + expiry).getBytes()));
            return "/api/catalog/stream/" + docId + "?token=" + token + "&expires=" + expiry;
        } catch (Exception e) { throw new BusinessException("Link generation failed", HttpStatus.INTERNAL_SERVER_ERROR); }
    }

    private DocumentResponse mapToDocDto(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .categoryName(doc.getCategory().getDisplayName())
                .documentType(doc.getContentType() != null ? doc.getContentType().name() : ContentTypes.STUDY_MATERIAL.name())
                .requiredTier(doc.getRequiredTier().name())
                .displayOrder(doc.getDisplayOrder())
                .createdAt(doc.getCreatedAt())
                .locked(true) // Initialized as locked, overwritten in business logic
                .build();
    }

    @Override @Transactional public void deleteDocument(UUID id) { documentRepository.deleteById(id); }
    @Override public List<Package> getAllPackages() { return packageRepository.findAll(); }
    @Override public CategoryResponse saveCategory(ExamCategory cat) { 
        ExamCategory s = categoryRepository.save(cat);
        return CategoryResponse.builder().id(s.getId()).displayName(s.getDisplayName()).description(s.getDescription()).build();
    }
    @Override public List<DocumentResponse> getUniversalFreeSamples() {
        return documentRepository.findByContentType(ContentTypes.SAMPLE_PDF).stream()
                .map(doc -> { DocumentResponse r = mapToDocDto(doc); r.setLocked(false); return r; }).collect(Collectors.toList());
    }
    @Override @Transactional public Package updatePackage(UUID id, PackageRequest details) {
        Package pkg = packageRepository.findById(id).orElseThrow();
        if (details.getName() != null) pkg.setName(details.getName());
        if (details.getMockTestLimit() != null) pkg.setMockTestLimit(details.getMockTestLimit());
        return packageRepository.save(pkg);
    }
    @Override public DocumentResponse updateDocumentMetadata(UUID id, String title, ContentTypes type, TierType tier, Integer order) {
        Document d = documentRepository.findById(id).orElseThrow();
        if (title != null) d.setTitle(title); 
        if (tier != null) d.setRequiredTier(tier); 
        if (order != null) d.setDisplayOrder(order); 
        if (type != null) d.setContentType(type);
        return mapToDocDto(documentRepository.save(d));
    }
    @Override @Transactional(readOnly = true)
    public List<PackageResponse> getPackagesByCategoryId(UUID categoryId) {
        return packageRepository.findByAccessibleCategoriesId(categoryId).stream().map(pkg -> PackageResponse.builder()
                .id(pkg.getUuid()).name(pkg.getName()).priceAmount(pkg.getPriceAmount()).durationInMonths(pkg.getDurationInMonths())
                .mockTestLimit(pkg.getMockTestLimit()).tierType(pkg.getTierType()).isCombo(pkg.getAccessibleCategories().size() > 1)
                .categoryNames(pkg.getAccessibleCategories().stream().map(ExamCategory::getDisplayName).collect(Collectors.toList())).build())
                .collect(Collectors.toList());
    }
    @Override public void verifySecureLink(UUID docId, UUID userId, String token, long expires) {
        if (System.currentTimeMillis() > expires) throw new BusinessException("Link expired", HttpStatus.GONE);
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(hmacSecret.getBytes(), "HmacSHA256"));
            String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((docId + ":" + userId + ":" + expires).getBytes()));
            if (!expected.equals(token)) throw new BusinessException("Invalid token", HttpStatus.FORBIDDEN);
        } catch (Exception e) { throw new BusinessException("Verification failed", HttpStatus.INTERNAL_SERVER_ERROR); }
    }
}