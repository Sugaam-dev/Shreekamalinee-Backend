package com.pmrgsolution.features.auth.controller;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.auth.dto.UserAccountDTO;

import com.pmrgsolution.features.auth.service.ProfileService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserAccountDTO> getMyAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(profileService.getUserAccountDetails(userDetails.getId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserAccountDTO> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @jakarta.validation.Valid @RequestBody UserAccountDTO updateRequest) {
        return ResponseEntity.ok(profileService.updateProfile(userDetails.getId(), updateRequest));
    }
}