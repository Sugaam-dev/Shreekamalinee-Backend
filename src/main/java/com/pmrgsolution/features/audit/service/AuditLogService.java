package com.pmrgsolution.features.audit.service;

import com.pmrgsolution.features.audit.dto.AuditEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void logEvent(String eventType, String userEmail, String ipAddress, String details);
    Page<AuditEventResponse> getAuditLogs(String eventType, String userEmail, Pageable pageable);
}