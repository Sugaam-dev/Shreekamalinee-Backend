package com.pmrgsolution.features.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(
    name = "product_variants",
    indexes = {
        @Index(name = "idx_variant_product", columnList = "product_id"),
        @Index(name = "idx_variant_sku", columnList = "sku")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "size", length = 50)
    private String size;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Version
    private Long version;

    public int getStock() {
        return stockQuantity != null ? stockQuantity : 0;
    }

    public void setStock(int stock) {
        this.stockQuantity = stock;
    }
}