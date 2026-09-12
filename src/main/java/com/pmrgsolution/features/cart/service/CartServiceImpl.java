package com.pmrgsolution.features.cart.service;

import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.exception.BusinessException;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.catalog.entity.Product;
import com.pmrgsolution.features.catalog.entity.ProductVariant;
import com.pmrgsolution.features.catalog.repository.ProductRepository;
import com.pmrgsolution.features.catalog.repository.ProductVariantRepository;
import com.pmrgsolution.features.cart.dto.*;
import com.pmrgsolution.features.cart.entity.*;
import com.pmrgsolution.features.cart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartDTO getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO addItemToCart(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        
        // 1. Resolve variant either directly by variantId or by productId fallback
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseGet(() -> {
                    // Fallback: check if the given UUID is a Product ID
                    Optional<Product> prodOpt = productRepository.findById(request.getVariantId());
                    if (prodOpt.isPresent() && prodOpt.get().getVariants() != null && !prodOpt.get().getVariants().isEmpty()) {
                        return prodOpt.get().getVariants().get(0);
                    }
                    throw new ResourceNotFoundException("Product Variant not found for ID: " + request.getVariantId());
                });

        int requestedQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;

        if (variant.getStockQuantity() != null && variant.getStockQuantity() < requestedQty) {
            throw new BusinessException("Requested quantity exceeds available stock (" + variant.getStockQuantity() + ")", HttpStatus.BAD_REQUEST);
        }

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(i -> i.getProductVariant() != null && i.getProductVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            int newQuantity = item.getQuantity() + requestedQty;
            if (variant.getStockQuantity() != null && variant.getStockQuantity() < newQuantity) {
                throw new BusinessException("Total cart quantity exceeds available stock (" + variant.getStockQuantity() + ")", HttpStatus.BAD_REQUEST);
            }
            item.setQuantity(newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(requestedQty)
                    .build();
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToCartDTO(savedCart);
    }

    @Override
    @Transactional
    public CartDTO updateItemQuantity(UUID userId, UUID itemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            cart.getItems().removeIf(i -> i.getId().equals(itemId));
        } else {
            ProductVariant variant = item.getProductVariant();
            if (variant.getStockQuantity() < quantity) {
                throw new BusinessException("Requested quantity exceeds available stock (" + variant.getStockQuantity() + ")", HttpStatus.BAD_REQUEST);
            }
            item.setQuantity(quantity);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToCartDTO(savedCart);
    }

    @Override
    @Transactional
    public CartDTO removeItemFromCart(UUID userId, UUID itemId) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() != null) {
            cart.getItems().removeIf(i -> i.getId().equals(itemId));
            cartRepository.save(cart);
        }
        return mapToCartDTO(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }


    // --- HELPERS ---

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = Cart.builder()
                    .user(user)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(cart);
        });
    }

    private CartDTO mapToCartDTO(Cart cart) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        java.util.List<CartItemDTO> itemDTOs = new ArrayList<>();

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                ProductVariant variant = item.getProductVariant();
                BigDecimal price = variant.getProduct().getOfferPrice() != null
                        ? variant.getProduct().getOfferPrice()
                        : variant.getProduct().getOriginalPrice();

                totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));

                String imageUrl = (variant.getProduct().getImageUrls() != null && !variant.getProduct().getImageUrls().isEmpty())
                        ? variant.getProduct().getImageUrls().get(0)
                        : null;

                itemDTOs.add(CartItemDTO.builder()
                        .id(item.getId())
                        .variantId(variant.getId())
                        .productName(variant.getProduct().getName())
                        .size(variant.getSize())
                        .color(variant.getColor())
                        .price(price)
                        .quantity(item.getQuantity())
                        .imageUrl(imageUrl)
                        .build());
            }
        }

        return CartDTO.builder()
                .id(cart.getId())
                .items(itemDTOs)
                .totalAmount(totalAmount)
                .build();
    }
}
