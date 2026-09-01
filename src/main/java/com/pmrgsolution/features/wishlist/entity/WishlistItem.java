package com.pmrgsolution.features.wishlist.entity;

import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.catalog.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "wishlist_items",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_id"})
    },
    indexes = {
        @Index(name = "idx_wishlist_user", columnList = "user_id"),
        @Index(name = "idx_wishlist_product", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
