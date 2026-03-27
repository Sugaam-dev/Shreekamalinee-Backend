package com.PMRGSolution.RENAISSANCE.features.auth.controller;

import com.PMRGSolution.RENAISSANCE.core.security.CustomUserDetails;
import com.PMRGSolution.RENAISSANCE.features.auth.dto.UserAccountDTO;

import com.PMRGSolution.RENAISSANCE.features.auth.service.ProfileService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserAccountDTO> getMyAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(profileService.getUserAccountDetails(userDetails.getId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<String> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserAccountDTO updateRequest) {
        profileService.updateProfile(userDetails.getId(), updateRequest);
        return ResponseEntity.ok("Profile updated successfully");
    }
}