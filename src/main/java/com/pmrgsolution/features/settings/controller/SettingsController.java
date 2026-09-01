package com.pmrgsolution.features.settings.controller;

import com.pmrgsolution.features.settings.dto.StoreSettingsResponse;
import com.pmrgsolution.features.settings.service.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final StoreSettingsService storeSettingsService;

    @GetMapping("/public")
    public ResponseEntity<StoreSettingsResponse> getPublicStoreSettings() {
        return ResponseEntity.ok(storeSettingsService.getStoreSettings());
    }
}