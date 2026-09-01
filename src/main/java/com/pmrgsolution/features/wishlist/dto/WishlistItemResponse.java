package com.pmrgsolution.features.wishlist.dto;

import com.pmrgsolution.features.catalog.dto.ProductDTO;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponse {
    private UUID id;
    private UUID productId;
    private ProductDTO product;
    private LocalDateTime addedAt;
}