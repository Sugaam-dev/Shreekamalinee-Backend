package com.pmrgsolution.features.settings.service;

import com.pmrgsolution.features.settings.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface StoreSettingsService {
    StoreSettingsResponse getStoreSettings();
    ShippingSettingsResponse updateShippingSettings(ShippingSettingsRequest request);
    AnnouncementSettingsResponse updateAnnouncementSettings(AnnouncementSettingsRequest request);
    ContactSettingsResponse updateContactSettings(ContactSettingsRequest request);
    StoreSettingsResponse updateBankingSettings(BankingSettingsRequest request);
    StoreSettingsResponse uploadQrCode(MultipartFile file);
}