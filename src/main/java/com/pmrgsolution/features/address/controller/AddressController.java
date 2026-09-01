package com.pmrgsolution.features.address.controller;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.address.dto.AddressRequest;
import com.pmrgsolution.features.address.dto.AddressResponse;
import com.pmrgsolution.features.address.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(addressService.addAddress(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(addressService.getAddresses(user.getId()));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID addressId,
            @AuthenticationPrincipal CustomUserDetails user) {
        addressService.deleteAddress(user.getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(addressService.updateAddress(user.getId(), addressId, request));
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable UUID addressId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(addressService.setDefaultAddress(user.getId(), addressId));
    }
}