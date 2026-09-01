package com.pmrgsolution.features.catalog.service;

import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.features.catalog.dto.*;
import com.pmrgsolution.features.catalog.entity.*;
import com.pmrgsolution.features.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pmrgsolution.core.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'main'")
    public List<CategoryResponse> getMainCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'sub_' + (#parentId != null ? #parentId.toString() : 'all')")
    public List<CategoryResponse> getSubcategories(UUID parentId) {
        List<Category> subcategories = (parentId != null)
                ? categoryRepository.findByParentCategoryId(parentId)
                : categoryRepository.findByParentCategoryIsNotNull();

        return subcategories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "products", "catalog"}, allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category not found"));
        }
        
        String inputSlug = request.getSlug() != null && !request.getSlug().isBlank() ? request.getSlug() : request.getName();
        String safeSlug = inputSlug.toLowerCase().trim()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replace(" ", "-");

        if (categoryRepository.findBySlug(safeSlug).isPresent()) {
            throw new BusinessException("Category with slug '" + safeSlug + "' already exists", HttpStatus.CONFLICT);
        }

        List<String> suggestedAttributes = request.getSuggestedAttributes();
        if ((suggestedAttributes == null || suggestedAttributes.isEmpty()) && parent != null) {
            suggestedAttributes = parent.getSuggestedAttributes();
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(safeSlug)
                .parentCategory(parent)
                .imageUrl(request.getImageUrl())
                .suggestedAttributes(suggestedAttributes)
                .build();
        return mapToCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "products", "catalog"}, allEntries = true)
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName().trim());
        }

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String safeSlug = request.getSlug().toLowerCase().trim()
                    .replaceAll("[^a-zA-Z0-9\\s]", "")
                    .replace(" ", "-");
            category.setSlug(safeSlug);
        } else if (request.getName() != null && !request.getName().isBlank()) {
            String safeSlug = request.getName().toLowerCase().trim()
                    .replaceAll("[^a-zA-Z0-9\\s]", "")
                    .replace(" ", "-");
            category.setSlug(safeSlug);
        }

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category not found"));
            category.setParentCategory(parent);
        }

        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl().trim());
        }

        if (request.getSuggestedAttributes() != null) {
            category.setSuggestedAttributes(request.getSuggestedAttributes());
        }

        return mapToCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "products", "catalog"}, allEntries = true)
    public void deleteCategory(UUID id) {
        log.info("REST request to delete category ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // 1. Safely unlink any products assigned to this category
        productRepository.unlinkCategoryFromProducts(id);

        // 2. Safely unlink any subcategories so they don't break foreign keys
        categoryRepository.unlinkParentCategory(id);

        // 3. Delete category
        String oldImageUrl = category.getImageUrl();
        categoryRepository.delete(category);

        // 4. Delete image from CDN
        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            try {
                fileStorageService.deleteFile(oldImageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete category image from storage on delete '{}': {}", oldImageUrl, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProducts(
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
            String sortBy) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdRecursive(categoryId);
        } else {
            products = productRepository.findAll();
        }

        java.util.stream.Stream<Product> stream = products.stream();

        // 1. Filter by Gender Category
        if (gender != null && !gender.isBlank()) {
            stream = stream.filter(p -> p.getGenderCategory() != null && p.getGenderCategory().equalsIgnoreCase(gender.trim()));
        }

        // 2. Filter by Brand Name
        if (brand != null && !brand.isBlank()) {
            stream = stream.filter(p -> p.getBrand() != null && p.getBrand().equalsIgnoreCase(brand.trim()));
        }

        // 3. Filter by Season Drop
        if (season != null && !season.isBlank()) {
            stream = stream.filter(p -> p.getSeason() != null && p.getSeason().equalsIgnoreCase(season.trim()));
        }

        // 4. Text Search (Matches Product Name or Description case-insensitively)
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase().trim();
            stream = stream.filter(p -> p.getName().toLowerCase().contains(q) || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)));
        }

        // 5. Price Bracket Range (Uses offerPrice if present, else originalPrice)
        if (minPrice != null) {
            stream = stream.filter(p -> {
                BigDecimal activePrice = (p.getOfferPrice() != null && p.getOfferPrice().compareTo(BigDecimal.ZERO) > 0) ? p.getOfferPrice() : p.getOriginalPrice();
                return activePrice != null && activePrice.compareTo(minPrice) >= 0;
            });
        }
        if (maxPrice != null) {
            stream = stream.filter(p -> {
                BigDecimal activePrice = (p.getOfferPrice() != null && p.getOfferPrice().compareTo(BigDecimal.ZERO) > 0) ? p.getOfferPrice() : p.getOriginalPrice();
                return activePrice != null && activePrice.compareTo(maxPrice) <= 0;
            });
        }

        // 6. Stock Availability Filter
        if (inStock != null) {
            stream = stream.filter(p -> {
                int totalStock = p.getVariants() != null ? p.getVariants().stream().mapToInt(ProductVariant::getStockQuantity).sum() : 0;
                return inStock ? totalStock > 0 : totalStock == 0;
            });
        }

        // 7. Sort by Newest (requires entity sorting before DTO map)
        if (sortBy != null && sortBy.equalsIgnoreCase("newest")) {
            stream = stream.sorted(java.util.Comparator.comparing(Product::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        }

        // Map to DTOs (includes calculated discount percentage and average rating)
        List<ProductDTO> mappedProducts = stream.map(this::mapToProductDTO).collect(Collectors.toList());

        // 8. Filter by Minimum Discount Percentage (e.g. minDiscount=20 for 20% or more)
        if (minDiscount != null && minDiscount > 0) {
            mappedProducts = mappedProducts.stream()
                    .filter(p -> p.getDiscountPercentage() != null && p.getDiscountPercentage() >= minDiscount)
                    .collect(Collectors.toList());
        }

        // 9. Filter by Minimum Customer Star Rating (e.g. minRating=4.0 for 4★ & above)
        if (minRating != null && minRating > 0.0) {
            mappedProducts = mappedProducts.stream()
                    .filter(p -> p.getAverageRating() != null && p.getAverageRating() >= minRating)
                    .collect(Collectors.toList());
        }

        // 10. Sort by Price (requires DTO sorting after map)
        if (sortBy != null && !sortBy.isBlank()) {
            if (sortBy.equalsIgnoreCase("priceasc")) {
                mappedProducts.sort(java.util.Comparator.comparing(p -> p.getOfferPrice() != null && p.getOfferPrice().compareTo(BigDecimal.ZERO) > 0 ? p.getOfferPrice() : p.getOriginalPrice()));
            } else if (sortBy.equalsIgnoreCase("pricedesc")) {
                mappedProducts.sort(java.util.Comparator.<ProductDTO, BigDecimal>comparing(p -> p.getOfferPrice() != null && p.getOfferPrice().compareTo(BigDecimal.ZERO) > 0 ? p.getOfferPrice() : p.getOriginalPrice()).reversed());
            }
        }

        return mappedProducts;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductDTO getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToProductDTO(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    public ProductDTO createProduct(ProductRequest request) {
        String productSku = request.getSku() != null ? request.getSku().trim().toUpperCase() : "";
        if (productSku.isBlank()) {
            throw new BusinessException("Product SKU is required", HttpStatus.BAD_REQUEST);
        }

        if (productRepository.existsBySku(productSku)) {
            throw new BusinessException("Product with SKU '" + productSku + "' already exists", HttpStatus.CONFLICT);
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .brand(request.getBrand())
                .originalPrice(request.getOriginalPrice())
                .offerPrice(request.getOfferPrice())
                .sku(productSku)
                .genderCategory(request.getGenderCategory().toUpperCase())
                .season(request.getSeason())
                .artisanalStory(request.getArtisanalStory())
                .fabricCare(request.getFabricCare())
                .shippingPolicy(request.getShippingPolicy())
                .category(category)
                .highlights(request.getHighlights())
                .aboutItem(request.getAboutItem())
                .build();

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ProductVariant> variants = new ArrayList<>();
            Set<String> usedSkus = new HashSet<>();
            for (VariantRequest vReq : request.getVariants()) {
                if (vReq.getSize() != null && vReq.getColor() != null) {
                    String variantSku = generateUniqueVariantSku(product, vReq, usedSkus);

                    ProductVariant variant = ProductVariant.builder()
                            .product(product)
                            .size(vReq.getSize().trim())
                            .color(vReq.getColor().trim())
                            .stockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0)
                            .sku(variantSku)
                            .build();
                    variants.add(variant);
                }
            }
            product.setVariants(variants);
        }

        Product savedProduct = productRepository.save(product);
        return mapToProductDTO(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String newProductSku = request.getSku() != null ? request.getSku().trim().toUpperCase() : product.getSku();
        if (productRepository.existsBySkuAndIdNot(newProductSku, id)) {
            throw new BusinessException("Product SKU '" + newProductSku + "' is already in use by another product", HttpStatus.CONFLICT);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setOfferPrice(request.getOfferPrice());
        product.setSku(newProductSku);
        product.setGenderCategory(request.getGenderCategory().toUpperCase());
        product.setSeason(request.getSeason());
        product.setArtisanalStory(request.getArtisanalStory());
        product.setFabricCare(request.getFabricCare());
        product.setShippingPolicy(request.getShippingPolicy());
        product.setCategory(category);

        if (request.getHighlights() != null) {
            if (product.getHighlights() == null) {
                product.setHighlights(new HashMap<>(request.getHighlights()));
            } else {
                product.getHighlights().clear();
                product.getHighlights().putAll(request.getHighlights());
            }
        }

        if (request.getAboutItem() != null) {
            if (product.getAboutItem() == null) {
                product.setAboutItem(new ArrayList<>(request.getAboutItem()));
            } else {
                product.getAboutItem().clear();
                product.getAboutItem().addAll(request.getAboutItem());
            }
        }

        if (request.getVariants() != null) {
            if (product.getVariants() == null) {
                product.setVariants(new ArrayList<>());
            }

            List<ProductVariant> existingVariants = product.getVariants();
            List<ProductVariant> updatedList = new ArrayList<>();
            Set<String> usedSkus = new HashSet<>();

            for (int i = 0; i < request.getVariants().size(); i++) {
                VariantRequest vReq = request.getVariants().get(i);
                if (vReq.getSize() != null && vReq.getColor() != null) {
                    String variantSku = generateUniqueVariantSku(product, vReq, usedSkus);

                    ProductVariant targetVariant = (i < existingVariants.size()) ? existingVariants.get(i) : null;

                    if (targetVariant != null) {
                        targetVariant.setSize(vReq.getSize().trim());
                        targetVariant.setColor(vReq.getColor().trim());
                        targetVariant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);
                        targetVariant.setSku(variantSku);
                        updatedList.add(targetVariant);
                    } else {
                        ProductVariant newVariant = ProductVariant.builder()
                                .product(product)
                                .size(vReq.getSize().trim())
                                .color(vReq.getColor().trim())
                                .stockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0)
                                .sku(variantSku)
                                .build();
                        updatedList.add(newVariant);
                    }
                }
            }

            existingVariants.clear();
            existingVariants.addAll(updatedList);
        }

        Product savedProduct = productRepository.save(product);
        return mapToProductDTO(savedProduct);
    }

    private String generateUniqueVariantSku(Product product, VariantRequest vReq, Set<String> usedSkus) {
        String baseSku;
        if (vReq.getSku() != null && !vReq.getSku().isBlank() && !vReq.getSku().trim().equalsIgnoreCase(product.getSku())) {
            baseSku = vReq.getSku().trim().toUpperCase();
        } else {
            String size = vReq.getSize() != null ? vReq.getSize().replaceAll("[^a-zA-Z0-9]", "").toUpperCase() : "FS";
            String color = vReq.getColor() != null ? vReq.getColor().replaceAll("[^a-zA-Z0-9]", "").toUpperCase() : "STD";
            baseSku = (product.getSku().trim() + "-" + size + "-" + color).toUpperCase();
        }

        String candidate = baseSku;
        int counter = 1;
        while (usedSkus.contains(candidate)) {
            candidate = baseSku + "-" + counter++;
        }
        usedSkus.add(candidate);
        return candidate;
    }


    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(UUID id) {
        log.info("REST request to delete product ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<String> imagesToDelete = product.getImageUrls() != null ? new ArrayList<>(product.getImageUrls()) : List.of();
        productRepository.delete(product);

        for (String imgUrl : imagesToDelete) {
            if (imgUrl != null && !imgUrl.isBlank()) {
                try {
                    fileStorageService.deleteFile(imgUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete product image '{}' on product delete: {}", imgUrl, e.getMessage());
                }
            }
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    public ProductVariantDTO addVariant(UUID productId, VariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (productVariantRepository.findBySku(request.getSku()).isPresent()) {
            throw new BusinessException("Variant with this SKU already exists", HttpStatus.BAD_REQUEST);
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(request.getSize().toUpperCase())
                .color(request.getColor())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .build();

        return mapToVariantDTO(productVariantRepository.save(variant));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    public ProductVariantDTO updateVariantStock(UUID variantId, Integer stock) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        if (stock < 0) {
            throw new BusinessException("Stock cannot be negative", HttpStatus.BAD_REQUEST);
        }

        variant.setStockQuantity(stock);
        return mapToVariantDTO(productVariantRepository.save(variant));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    public void deleteVariant(UUID variantId) {
        if (!productVariantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Variant not found");
        }
        productVariantRepository.deleteById(variantId);
    }

    // --- MAPPER HELPERS ---

    private CategoryResponse mapToCategoryResponse(Category category) {
        List<String> attributes = category.getSuggestedAttributes() != null
                ? new ArrayList<>(category.getSuggestedAttributes())
                : new ArrayList<>();
        if (attributes.isEmpty() && category.getParentCategory() != null && category.getParentCategory().getSuggestedAttributes() != null) {
            attributes = new ArrayList<>(category.getParentCategory().getSuggestedAttributes());
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .parentCategoryName(category.getParentCategory() != null ? category.getParentCategory().getName() : null)
                .imageUrl(category.getImageUrl())
                .suggestedAttributes(attributes)
                .build();
    }

    private ProductDTO mapToProductDTO(Product product) {
        List<ProductVariantDTO> variants = product.getVariants() != null
                ? product.getVariants().stream().map(this::mapToVariantDTO).collect(Collectors.toList())
                : List.of();

        int totalStock = variants.stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum();
        boolean inStock = variants.isEmpty() || totalStock > 0;

        Integer discountPercentage = null;
        if (product.getOriginalPrice() != null && product.getOfferPrice() != null && product.getOriginalPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal diff = product.getOriginalPrice().subtract(product.getOfferPrice());
            if (diff.compareTo(java.math.BigDecimal.ZERO) > 0) {
                discountPercentage = diff.multiply(java.math.BigDecimal.valueOf(100))
                        .divide(product.getOriginalPrice(), 0, java.math.RoundingMode.HALF_UP)
                        .intValue();
            }
        }

        String parentCategoryName = null;
        if (product.getCategory() != null && product.getCategory().getParentCategory() != null) {
            parentCategoryName = product.getCategory().getParentCategory().getName();
        }

        Double averageRating = reviewRepository.getAverageRating(product.getId());
        Long totalReviews = reviewRepository.countByProductId(product.getId());
        if (averageRating != null) {
            averageRating = Math.round(averageRating * 10.0) / 10.0;
        }

        java.util.Map<String, String> highlights = product.getHighlights() != null
                ? new java.util.HashMap<>(product.getHighlights())
                : new java.util.HashMap<>();
        List<String> aboutItem = product.getAboutItem() != null
                ? new ArrayList<>(product.getAboutItem())
                : new ArrayList<>();
        List<String> imageUrls = product.getImageUrls() != null
                ? new ArrayList<>(product.getImageUrls())
                : new ArrayList<>();

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .originalPrice(product.getOriginalPrice())
                .offerPrice(product.getOfferPrice())
                .sku(product.getSku())
                .genderCategory(product.getGenderCategory())
                .season(product.getSeason())
                .artisanalStory(product.getArtisanalStory())
                .fabricCare(product.getFabricCare())
                .shippingPolicy(product.getShippingPolicy())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .parentCategoryName(parentCategoryName)
                .discountPercentage(discountPercentage)
                .inStock(inStock)
                .totalStock(totalStock)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .totalReviews(totalReviews != null ? totalReviews : 0L)
                .highlights(highlights)
                .aboutItem(aboutItem)
                .imageUrls(imageUrls)
                .variants(variants)
                .build();
    }

    private ProductVariantDTO mapToVariantDTO(ProductVariant variant) {
        return ProductVariantDTO.builder()
                .id(variant.getId())
                .size(variant.getSize())
                .color(variant.getColor())
                .stockQuantity(variant.getStockQuantity())
                .sku(variant.getSku())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "catalog"}, allEntries = true)
    public ProductDTO uploadProductImage(UUID productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String imageUrl = fileStorageService.storeFile(file, "products");

        if (product.getImageUrls() == null) {
            product.setImageUrls(new ArrayList<>());
        }
        product.getImageUrls().add(imageUrl);

        return mapToProductDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO deleteProductImage(UUID productId, String imageUrl) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getImageUrls() != null && product.getImageUrls().remove(imageUrl)) {
            Product saved = productRepository.save(product);
            try {
                fileStorageService.deleteFile(imageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete product image from storage '{}': {}", imageUrl, e.getMessage());
            }
            return mapToProductDTO(saved);
        }
        return mapToProductDTO(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "products", "catalog"}, allEntries = true)
    public CategoryResponse uploadCategoryImage(UUID categoryId, MultipartFile file) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String oldImageUrl = category.getImageUrl();
        String newImageUrl = fileStorageService.storeFile(file, "categories");
        category.setImageUrl(newImageUrl);
        Category saved = categoryRepository.save(category);

        // Clean up previous category image from Supabase
        if (oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(newImageUrl)) {
            try {
                fileStorageService.deleteFile(oldImageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete previous category image from storage '{}': {}", oldImageUrl, e.getMessage());
            }
        }

        return mapToCategoryResponse(saved);
    }
}
