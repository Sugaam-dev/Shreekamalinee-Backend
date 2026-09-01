package com.pmrgsolution.features.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_prod_category", columnList = "category_id"),
        @Index(name = "idx_prod_season", columnList = "season"),
        @Index(name = "idx_prod_gender", columnList = "gender_category"),
        @Index(name = "idx_prod_brand", columnList = "brand"),
        @Index(name = "idx_prod_created", columnList = "created_at"),
        @Index(name = "idx_prod_sku", columnList = "sku")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "original_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "offer_price", precision = 10, scale = 2)
    private BigDecimal offerPrice;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "gender_category", length = 50)
    private String genderCategory;

    @Column(name = "season", length = 50)
    private String season;

    @Column(name = "artisanal_story", columnDefinition = "TEXT")
    private String artisanalStory;

    @Column(name = "fabric_care", columnDefinition = "TEXT")
    private String fabricCare;

    @Column(name = "shipping_policy", columnDefinition = "TEXT")
    private String shippingPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> imageUrls;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_highlights", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "highlight_key")
    @Column(name = "highlight_value", columnDefinition = "TEXT")
    private java.util.Map<String, String> highlights;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_about_items", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "point", columnDefinition = "TEXT")
    private List<String> aboutItem;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getPrice() {
        return originalPrice;
    }

    public int getStock() {
        if (variants == null || variants.isEmpty()) return 0;
        return variants.stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum();
    }

    public void setStock(int stock) {
        // Product stock is calculated dynamically from variants.
        // Individual variant stock must be updated via ProductVariant directly.
    }
}