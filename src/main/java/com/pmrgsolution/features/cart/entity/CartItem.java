package com.pmrgsolution.features.cart.entity;

import com.pmrgsolution.features.catalog.entity.Product;
import com.pmrgsolution.features.catalog.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(name = "idx_cart_item_cart", columnList = "cart_id"),
        @Index(name = "idx_cart_item_variant", columnList = "variant_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public Product getProduct() {
        return productVariant != null ? productVariant.getProduct() : null;
    }

    public ProductVariant getVariant() {
        return productVariant;
    }

    public String getSize() {
        return productVariant != null ? productVariant.getSize() : null;
    }

    public String getColor() {
        return productVariant != null ? productVariant.getColor() : null;
    }
}