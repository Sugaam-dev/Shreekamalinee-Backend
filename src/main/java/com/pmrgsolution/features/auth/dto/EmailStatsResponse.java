package com.pmrgsolution.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailStatsResponse {
    private long sentToday;
    private long dailyLimit;
    private long remainingToday;
    private boolean dailyQuotaExceeded;
    private long sentThisMonth;
    private long monthlyLimit;
    private long remainingThisMonth;
    private boolean monthlyQuotaExceeded;
    private String quotaResetTime;
}
