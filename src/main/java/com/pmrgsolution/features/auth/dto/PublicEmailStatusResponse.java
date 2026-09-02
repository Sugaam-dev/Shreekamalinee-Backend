package com.pmrgsolution.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicEmailStatusResponse {
    private boolean dailyQuotaExceeded;
    private boolean serviceActive;
    private String announcement;
    private String quotaResetTime;
}
