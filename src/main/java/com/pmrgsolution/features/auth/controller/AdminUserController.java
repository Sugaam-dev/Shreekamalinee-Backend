package com.pmrgsolution.features.auth.controller;

import com.pmrgsolution.features.auth.dto.AdminUserDTO;
import com.pmrgsolution.features.auth.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserDTO>> searchUsers(
            @RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(adminUserService.searchUsers(query));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserDTO> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam("enabled") boolean enabled) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(userId, enabled));
    }
}
