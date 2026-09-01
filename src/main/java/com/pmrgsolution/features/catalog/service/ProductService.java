package com.pmrgsolution.features.catalog.service;

import com.pmrgsolution.features.catalog.dto.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getMainCategories();
    List<CategoryResponse> getSubcategories(UUID parentId);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(UUID id, CategoryRequest request);
    void deleteCategory(UUID id);

    List<ProductDTO> getProducts(
            UUID categoryId,
            String gender,
            String brand,
            String search,
            String season,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            Double minRating,
            Integer minDiscount,
            String sortBy);

    ProductDTO getProductById(UUID id);
    ProductDTO createProduct(ProductRequest request);
    ProductDTO updateProduct(UUID id, ProductRequest request);
    void deleteProduct(UUID id);

    ProductVariantDTO addVariant(UUID productId, VariantRequest request);
    ProductVariantDTO updateVariantStock(UUID variantId, Integer stock);
    void deleteVariant(UUID variantId);

    ProductDTO uploadProductImage(UUID productId, org.springframework.web.multipart.MultipartFile file);
    ProductDTO deleteProductImage(UUID productId, String imageUrl);
    CategoryResponse uploadCategoryImage(UUID categoryId, org.springframework.web.multipart.MultipartFile file);
}
