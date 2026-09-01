package com.pmrgsolution.features.cart.controller;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.cart.dto.AddToCartRequest;
import com.pmrgsolution.features.cart.dto.CartDTO;
import com.pmrgsolution.features.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDTO> getCart(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(cartService.getCart(user.getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(cartService.addItemToCart(user.getId(), request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateItemQuantity(
            @PathVariable UUID itemId,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(cartService.updateItemQuantity(user.getId(), itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeItemFromCart(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(cartService.removeItemFromCart(user.getId(), itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal CustomUserDetails user) {
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}
