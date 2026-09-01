package com.pmrgsolution.features.catalog.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID productId;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private Integer rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;
}
