package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.auth.dto.EmailStatsResponse;
import com.pmrgsolution.features.auth.dto.PublicEmailStatusResponse;

public interface EmailUsageService {
    void recordEmailSent();
    void recordEmailFailed();
    EmailStatsResponse getEmailStats();
    PublicEmailStatusResponse getPublicEmailStatus();
}
