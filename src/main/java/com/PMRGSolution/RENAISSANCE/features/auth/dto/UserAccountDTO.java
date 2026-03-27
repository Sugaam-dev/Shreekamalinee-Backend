package com.PMRGSolution.RENAISSANCE.features.auth.dto;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountDTO {
    private UUID userId;
    private String firstName; 
    private String lastName;  
    private String email;
    private String phone; // Maps to phone_No in DB
    private boolean isEmailVerified;
    private List<SubscriptionBrief> activeSubscriptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionBrief {
        private UUID categoryId;
        private String categoryName;
        private TierType tier;
        private LocalDateTime expiryDate;
        private long daysRemaining;
        private boolean isExpired;
    }
}