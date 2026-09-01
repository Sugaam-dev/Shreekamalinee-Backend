package com.pmrgsolution.features.wishlist.service;

import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.catalog.dto.ProductDTO;
import com.pmrgsolution.features.catalog.entity.Product;
import com.pmrgsolution.features.catalog.repository.ProductRepository;
import com.pmrgsolution.features.catalog.service.ProductService;
import com.pmrgsolution.features.wishlist.dto.WishlistItemResponse;
import com.pmrgsolution.features.wishlist.dto.WishlistResponse;
import com.pmrgsolution.features.wishlist.entity.WishlistItem;
import com.pmrgsolution.features.wishlist.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getWishlist(UUID userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(item -> productService.getProductById(item.getProduct().getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlistItems(UUID userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(item -> WishlistItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .product(productService.getProductById(item.getProduct().getId()))
                        .addedAt(item.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponse getWishlistResponse(UUID userId) {
        List<ProductDTO> products = getWishlist(userId);
        return WishlistResponse.builder()
                .totalCount(products.size())
                .items(products)
                .build();
    }

    @Override
    @Transactional
    public void addToWishlist(UUID userId, UUID productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException("Product is already in your wishlist", HttpStatus.CONFLICT);
        }

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();

        wishlistRepository.save(item);
    }

    @Override
    @Transactional
    public void removeFromWishlist(UUID userId, UUID productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Product not in wishlist");
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWishlisted(UUID userId, UUID productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
}