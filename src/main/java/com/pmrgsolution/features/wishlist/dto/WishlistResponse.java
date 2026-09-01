package com.pmrgsolution.features.wishlist.dto;

import com.pmrgsolution.features.catalog.dto.ProductDTO;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponse {
    private Integer totalCount;
    private List<ProductDTO> items;
}