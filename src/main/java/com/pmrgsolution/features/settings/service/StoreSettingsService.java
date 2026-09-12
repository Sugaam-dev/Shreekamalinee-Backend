package com.pmrgsolution.features.settings.service;

import com.pmrgsolution.features.settings.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface StoreSettingsService {
    StoreSettingsResponse getPublicStoreSettings();
    StoreSettingsResponse getStoreSettings();
    BankDetailsResponse getBankDetails();
    ShippingSettingsResponse updateShippingSettings(ShippingSettingsRequest request);
    AnnouncementSettingsResponse updateAnnouncementSettings(AnnouncementSettingsRequest request);
    ContactSettingsResponse updateContactSettings(ContactSettingsRequest request);
    StoreSettingsResponse updateBankingSettings(BankingSettingsRequest request);
    StoreSettingsResponse uploadQrCode(MultipartFile file);
}