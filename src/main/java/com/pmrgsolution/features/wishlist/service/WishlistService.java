package com.pmrgsolution.features.wishlist.service;

import com.pmrgsolution.features.catalog.dto.ProductDTO;
import com.pmrgsolution.features.wishlist.dto.WishlistItemResponse;
import com.pmrgsolution.features.wishlist.dto.WishlistResponse;
import java.util.List;
import java.util.UUID;

public interface WishlistService {
    List<ProductDTO> getWishlist(UUID userId);
    List<WishlistItemResponse> getWishlistItems(UUID userId);
    WishlistResponse getWishlistResponse(UUID userId);
    void addToWishlist(UUID userId, UUID productId);
    void removeFromWishlist(UUID userId, UUID productId);
    boolean isWishlisted(UUID userId, UUID productId);
}