package com.pmrgsolution.features.wishlist.controller;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.cart.dto.CartDTO;
import com.pmrgsolution.features.catalog.dto.ProductDTO;
import com.pmrgsolution.features.wishlist.dto.WishlistItemResponse;
import com.pmrgsolution.features.wishlist.dto.WishlistResponse;
import com.pmrgsolution.features.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getWishlist(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(wishlistService.getWishlist(userDetails.getId()));
    }

    @GetMapping("/items")
    public ResponseEntity<List<WishlistItemResponse>> getWishlistItems(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(wishlistService.getWishlistItems(userDetails.getId()));
    }

    @GetMapping("/details")
    public ResponseEntity<WishlistResponse> getWishlistDetails(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(wishlistService.getWishlistResponse(userDetails.getId()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<String> addToWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        wishlistService.addToWishlist(userDetails.getId(), productId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Product added to wishlist successfully");
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        wishlistService.removeFromWishlist(userDetails.getId(), productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/move-to-cart")
    public ResponseEntity<CartDTO> moveToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID variantId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        CartDTO cart = wishlistService.moveToCart(userDetails.getId(), productId, variantId, quantity);
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/{productId}/status")
    public ResponseEntity<Boolean> isWishlisted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(wishlistService.isWishlisted(userDetails.getId(), productId));
    }
}