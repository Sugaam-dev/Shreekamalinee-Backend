package com.pmrgsolution.features.settings.controller;

import com.pmrgsolution.features.settings.dto.*;
import com.pmrgsolution.features.settings.service.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final StoreSettingsService storeSettingsService;

    @GetMapping
    public ResponseEntity<StoreSettingsResponse> getStoreSettingsAdmin() {
        return ResponseEntity.ok(storeSettingsService.getStoreSettings());
    }

    @PutMapping("/shipping")
    public ResponseEntity<ShippingSettingsResponse> updateShippingSettings(@RequestBody ShippingSettingsRequest request) {
        return ResponseEntity.ok(storeSettingsService.updateShippingSettings(request));
    }

    @PutMapping("/announcement")
    public ResponseEntity<AnnouncementSettingsResponse> updateAnnouncementSettings(@RequestBody AnnouncementSettingsRequest request) {
        return ResponseEntity.ok(storeSettingsService.updateAnnouncementSettings(request));
    }

    @PutMapping("/contact")
    public ResponseEntity<ContactSettingsResponse> updateContactSettings(@RequestBody ContactSettingsRequest request) {
        return ResponseEntity.ok(storeSettingsService.updateContactSettings(request));
    }

    @PutMapping("/banking")
    public ResponseEntity<StoreSettingsResponse> updateBankingSettings(@RequestBody BankingSettingsRequest request) {
        return ResponseEntity.ok(storeSettingsService.updateBankingSettings(request));
    }

    @PostMapping("/banking/qr-code")
    public ResponseEntity<StoreSettingsResponse> uploadQrCode(@RequestParam("image") MultipartFile file) {
        return ResponseEntity.ok(storeSettingsService.uploadQrCode(file));
    }
}