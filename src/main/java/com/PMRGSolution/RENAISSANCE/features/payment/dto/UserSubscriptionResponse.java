package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSubscriptionResponse {
    private UUID id;
    private String userEmail;
    private String categoryDisplayName;
    private String tier;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private boolean active;
}
