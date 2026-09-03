package com.pmrgsolution.features.settings.service;

import com.pmrgsolution.core.service.FileStorageService;
import com.pmrgsolution.features.settings.dto.*;
import com.pmrgsolution.features.settings.entity.StoreSettings;
import com.pmrgsolution.features.settings.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreSettingsServiceImpl implements StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "public_store_settings", key = "'active'")
    public StoreSettingsResponse getPublicStoreSettings() {
        return storeSettingsRepository.findFirstByOrderByCreatedAtDesc()
                .map(this::mapToPublicResponse)
                .orElseGet(() -> mapToPublicResponse(createDefaultSettingsEntity()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "bank_details", key = "'active'")
    public StoreSettingsResponse getStoreSettings() {
        return storeSettingsRepository.findFirstByOrderByCreatedAtDesc()
                .map(this::mapToResponse)
                .orElseGet(() -> mapToResponse(createDefaultSettingsEntity()));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "bank_details", allEntries = true),
        @CacheEvict(value = "public_store_settings", allEntries = true)
    })
    public ShippingSettingsResponse updateShippingSettings(ShippingSettingsRequest request) {
        StoreSettings entity = storeSettingsRepository.findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> StoreSettings.builder().build());

        if (request.getFreeShippingThreshold() != null) entity.setFreeShippingThreshold(request.getFreeShippingThreshold());
        if (request.getStandardShippingFee() != null) entity.setStandardShippingFee(request.getStandardShippingFee());
        if (request.getCodHandlingFee() != null) entity.setCodHandlingFee(request.getCodHandlingFee());
        if (request.getFreeCodThreshold() != null) entity.setFreeCodThreshold(request.getFreeCodThreshold());
        if (request.getIsFreeShippingPromoActive() != null) entity.setIsFreeShippingPromoActive(request.getIsFreeShippingPromoActive());
        if (request.getEstimatedDeliveryDaysMin() != null) entity.setEstimatedDeliveryDaysMin(request.getEstimatedDeliveryDaysMin());
        if (request.getEstimatedDeliveryDaysMax() != null) entity.setEstimatedDeliveryDaysMax(request.getEstimatedDeliveryDaysMax());
        if (request.getReturnWindowDays() != null) entity.setReturnWindowDays(request.getReturnWindowDays());
        if (request.getIsReturnActive() != null) entity.setIsReturnActive(request.getIsReturnActive());
        if (request.getReturnPolicyText() != null) entity.setReturnPolicyText(request.getReturnPolicyText().trim());
        if (request.getDeliveryPolicyNotice() != null) entity.setDeliveryPolicyNotice(request.getDeliveryPolicyNotice().trim());

        StoreSettings saved = storeSettingsRepository.save(entity);
        return ShippingSettingsResponse.builder()
                .freeShippingThreshold(saved.getFreeShippingThreshold())
                .standardShippingFee(saved.getStandardShippingFee())
                .codHandlingFee(saved.getCodHandlingFee())
                .freeCodThreshold(saved.getFreeCodThreshold())
                .isFreeShippingPromoActive(saved.getIsFreeShippingPromoActive())
                .estimatedDeliveryDaysMin(saved.getEstimatedDeliveryDaysMin())
                .estimatedDeliveryDaysMax(saved.getEstimatedDeliveryDaysMax())
                .returnWindowDays(saved.getReturnWindowDays())
                .isReturnActive(saved.getIsReturnActive())
                .returnPolicyText(saved.getReturnPolicyText())
                .deliveryPolicyNotice(saved.getDeliveryPolicyNotice())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "bank_details", allEntries = true),
        @CacheEvict(value = "public_store_settings", allEntries = true)
    })
    public AnnouncementSettingsResponse updateAnnouncementSettings(AnnouncementSettingsRequest request) {
        StoreSettings entity = storeSettingsRepository.findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> StoreSettings.builder().build());

        if (request.getIsAnnouncementActive() != null) entity.setIsAnnouncementActive(request.getIsAnnouncementActive());
        if (request.getAnnouncementText() != null) entity.setAnnouncementText(request.getAnnouncementText().trim());
        if (request.getAnnouncementLink() != null) entity.setAnnouncementLink(request.getAnnouncementLink().trim());
        if (request.getAnnouncementsJson() != null) entity.setAnnouncementsJson(request.getAnnouncementsJson().trim());

        StoreSettings saved = storeSettingsRepository.save(entity);
        return AnnouncementSettingsResponse.builder()
                .isAnnouncementActive(saved.getIsAnnouncementActive())
                .announcementText(saved.getAnnouncementText())
                .announcementLink(saved.getAnnouncementLink())
                .announcementsJson(saved.getAnnouncementsJson())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "bank_details", allEntries = true),
        @CacheEvict(value = "public_store_settings", allEntries = true)
    })
    public ContactSettingsResponse updateContactSettings(ContactSettingsRequest request) {
        StoreSettings entity = storeSettingsRepository.findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> StoreSettings.builder().build());

        if (request.getWhatsappNumber() != null) entity.setWhatsappNumber(request.getWhatsappNumber().trim());
        if (request.getSupportEmail() != null) entity.setSupportEmail(request.getSupportEmail().trim());
        if (request.getContactAddress() != null) entity.setContactAddress(request.getContactAddress().trim());
        if (request.getOperatingHours() != null) entity.setOperatingHours(request.getOperatingHours().trim());

        StoreSettings saved = storeSettingsRepository.save(entity);
        return ContactSettingsResponse.builder()
                .whatsappNumber(saved.getWhatsappNumber())
                .supportEmail(saved.getSupportEmail())
                .contactAddress(saved.getContactAddress())
                .operatingHours(saved.getOperatingHours())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "bank_details", allEntries = true),
        @CacheEvict(value = "public_store_settings", allEntries = true)
    })
    public StoreSettingsResponse updateBankingSettings(BankingSettingsRequest request) {
        StoreSettings entity = storeSettingsRepository.findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> StoreSettings.builder().build());

        if (request.getAccountHolderName() != null) entity.setAccountHolderName(request.getAccountHolderName().trim());
        if (request.getAccountNumber() != null) entity.setAccountNumber(request.getAccountNumber().trim());
        if (request.getIfscCode() != null) entity.setIfscCode(request.getIfscCode().trim().toUpperCase());
        if (request.getBankName() != null) entity.setBankName(request.getBankName().trim());
        if (request.getBranchName() != null) entity.setBranchName(request.getBranchName().trim());
        if (request.getUpiId() != null) entity.setUpiId(request.getUpiId().trim());
        if (request.getIsUpiPaymentActive() != null) entity.setIsUpiPaymentActive(request.getIsUpiPaymentActive());
        // Razorpay is not implemented - permanently enforce inactive in store configuration
        entity.setIsRazorpayPaymentActive(false);
        if (request.getIsCodPaymentActive() != null) entity.setIsCodPaymentActive(request.getIsCodPaymentActive());
        if (request.getIsWhatsappOrderActive() != null) entity.setIsWhatsappOrderActive(request.getIsWhatsappOrderActive());
        if (request.getEstimatedDeliveryDaysMin() != null) entity.setEstimatedDeliveryDaysMin(request.getEstimatedDeliveryDaysMin());
        if (request.getEstimatedDeliveryDaysMax() != null) entity.setEstimatedDeliveryDaysMax(request.getEstimatedDeliveryDaysMax());
        if (request.getReturnWindowDays() != null) entity.setReturnWindowDays(request.getReturnWindowDays());
        if (request.getIsReturnActive() != null) entity.setIsReturnActive(request.getIsReturnActive());
        if (request.getReturnPolicyText() != null) entity.setReturnPolicyText(request.getReturnPolicyText().trim());
        if (request.getDeliveryPolicyNotice() != null) entity.setDeliveryPolicyNotice(request.getDeliveryPolicyNotice().trim());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());

        return mapToResponse(storeSettingsRepository.save(entity));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "bank_details", allEntries = true),
        @CacheEvict(value = "public_store_settings", allEntries = true)
    })
    public StoreSettingsResponse uploadQrCode(MultipartFile file) {
        StoreSettings entity = storeSettingsRepository.findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> StoreSettings.builder().build());

        String oldQrUrl = entity.getQrCodeUrl();
        String newQrUrl = fileStorageService.storeFile(file, "payments");
        entity.setQrCodeUrl(newQrUrl);
        StoreSettings saved = storeSettingsRepository.save(entity);

        if (oldQrUrl != null && !oldQrUrl.isBlank() && !oldQrUrl.equals(newQrUrl)) {
            try {
                fileStorageService.deleteFile(oldQrUrl);
            } catch (Exception e) {
                log.warn("Failed to delete previous QR code from storage '{}': {}", oldQrUrl, e.getMessage());
            }
        }

        return mapToResponse(saved);
    }

    private StoreSettings createDefaultSettingsEntity() {
        return StoreSettings.builder()
                .accountHolderName("Shreekamalinee Luxury Sarees")
                .accountNumber("")
                .ifscCode("")
                .bankName("")
                .branchName("")
                .upiId("")
                .qrCodeUrl("")
                .whatsappNumber("+919876543210")
                .supportEmail("care@shreekamalinee.com")
                .freeShippingThreshold(java.math.BigDecimal.valueOf(1499.00))
                .standardShippingFee(java.math.BigDecimal.valueOf(99.00))
                .codHandlingFee(java.math.BigDecimal.valueOf(99.00))
                .freeCodThreshold(java.math.BigDecimal.valueOf(2999.00))
                .isFreeShippingPromoActive(false)
                .isAnnouncementActive(true)
                .announcementText("✨ Festive Handloom Edit Live — Free Express Shipping on Orders Above ₹1,499 ✨")
                .announcementLink("/shop")
                .contactAddress("Shreekamalinee Studio, Atelier Heritage Lane, Varanasi, Uttar Pradesh 221001, India")
                .operatingHours("Monday to Saturday: 10:00 AM – 7:00 PM IST")
                .isActive(true)
                .build();
    }

    /**
     * Sanitized public response: OMIT all sensitive banking credentials.
     */
    private StoreSettingsResponse mapToPublicResponse(StoreSettings entity) {
        return StoreSettingsResponse.builder()
                .id(entity.getId())
                // Omit banking details from public payload
                .accountHolderName(null)
                .accountNumber(null)
                .ifscCode(null)
                .bankName(null)
                .branchName(null)
                .upiId(null)
                .qrCodeUrl(null)
                // Public customer-facing channels & policies
                .whatsappNumber(entity.getWhatsappNumber())
                .supportEmail(entity.getSupportEmail())
                .contactAddress(entity.getContactAddress())
                .operatingHours(entity.getOperatingHours())
                .contactPhone(entity.getWhatsappNumber())
                .contactEmail(entity.getSupportEmail())
                .freeShippingThreshold(entity.getFreeShippingThreshold())
                .standardShippingFee(entity.getStandardShippingFee())
                .codHandlingFee(entity.getCodHandlingFee())
                .freeCodThreshold(entity.getFreeCodThreshold())
                .isFreeShippingPromoActive(entity.getIsFreeShippingPromoActive())
                .isAnnouncementActive(entity.getIsAnnouncementActive())
                .announcementText(entity.getAnnouncementText())
                .announcementsJson(entity.getAnnouncementsJson())
                .announcementLink(entity.getAnnouncementLink())
                .isUpiPaymentActive(entity.getIsUpiPaymentActive() != null ? entity.getIsUpiPaymentActive() : true)
                .isRazorpayPaymentActive(false)
                .isRazorpayImplemented(false)
                .isCodPaymentActive(entity.getIsCodPaymentActive() != null ? entity.getIsCodPaymentActive() : true)
                .isWhatsappOrderActive(entity.getIsWhatsappOrderActive() != null ? entity.getIsWhatsappOrderActive() : true)
                .estimatedDeliveryDaysMin(entity.getEstimatedDeliveryDaysMin() != null ? entity.getEstimatedDeliveryDaysMin() : 3)
                .estimatedDeliveryDaysMax(entity.getEstimatedDeliveryDaysMax() != null ? entity.getEstimatedDeliveryDaysMax() : 5)
                .returnWindowDays(entity.getReturnWindowDays() != null ? entity.getReturnWindowDays() : 7)
                .isReturnActive(entity.getIsReturnActive() != null ? entity.getIsReturnActive() : true)
                .returnPolicyText(entity.getReturnPolicyText())
                .deliveryPolicyNotice(entity.getDeliveryPolicyNotice())
                .isActive(entity.getIsActive())
                .build();
    }

    /**
     * Admin full response: includes all banking and store parameters.
     */
    private StoreSettingsResponse mapToResponse(StoreSettings entity) {
        return StoreSettingsResponse.builder()
                .id(entity.getId())
                .accountHolderName(entity.getAccountHolderName())
                .accountNumber(entity.getAccountNumber())
                .ifscCode(entity.getIfscCode())
                .bankName(entity.getBankName())
                .branchName(entity.getBranchName())
                .upiId(entity.getUpiId())
                .qrCodeUrl(entity.getQrCodeUrl())
                .whatsappNumber(entity.getWhatsappNumber())
                .supportEmail(entity.getSupportEmail())
                .contactAddress(entity.getContactAddress())
                .operatingHours(entity.getOperatingHours())
                .contactPhone(entity.getWhatsappNumber())
                .contactEmail(entity.getSupportEmail())
                .freeShippingThreshold(entity.getFreeShippingThreshold())
                .standardShippingFee(entity.getStandardShippingFee())
                .codHandlingFee(entity.getCodHandlingFee())
                .freeCodThreshold(entity.getFreeCodThreshold())
                .isFreeShippingPromoActive(entity.getIsFreeShippingPromoActive())
                .isAnnouncementActive(entity.getIsAnnouncementActive())
                .announcementText(entity.getAnnouncementText())
                .announcementsJson(entity.getAnnouncementsJson())
                .announcementLink(entity.getAnnouncementLink())
                .isUpiPaymentActive(entity.getIsUpiPaymentActive() != null ? entity.getIsUpiPaymentActive() : true)
                .isRazorpayPaymentActive(false)
                .isRazorpayImplemented(false)
                .isCodPaymentActive(entity.getIsCodPaymentActive() != null ? entity.getIsCodPaymentActive() : true)
                .isWhatsappOrderActive(entity.getIsWhatsappOrderActive() != null ? entity.getIsWhatsappOrderActive() : true)
                .estimatedDeliveryDaysMin(entity.getEstimatedDeliveryDaysMin() != null ? entity.getEstimatedDeliveryDaysMin() : 3)
                .estimatedDeliveryDaysMax(entity.getEstimatedDeliveryDaysMax() != null ? entity.getEstimatedDeliveryDaysMax() : 5)
                .returnWindowDays(entity.getReturnWindowDays() != null ? entity.getReturnWindowDays() : 7)
                .isReturnActive(entity.getIsReturnActive() != null ? entity.getIsReturnActive() : true)
                .returnPolicyText(entity.getReturnPolicyText())
                .deliveryPolicyNotice(entity.getDeliveryPolicyNotice())
                .isActive(entity.getIsActive())
                .build();
    }
}