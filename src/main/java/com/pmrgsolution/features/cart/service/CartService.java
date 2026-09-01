package com.pmrgsolution.features.cart.service;

import com.pmrgsolution.features.cart.dto.AddToCartRequest;
import com.pmrgsolution.features.cart.dto.CartDTO;
import java.util.UUID;

public interface CartService {
    CartDTO getCart(UUID userId);
    CartDTO addItemToCart(UUID userId, AddToCartRequest request);
    CartDTO updateItemQuantity(UUID userId, UUID itemId, Integer quantity);
    CartDTO removeItemFromCart(UUID userId, UUID itemId);
    void clearCart(UUID userId);
}
