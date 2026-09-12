package com.pmrgsolution.features.audit.service;

import com.pmrgsolution.features.audit.dto.AuditEventResponse;
import com.pmrgsolution.features.audit.entity.AuditEvent;
import com.pmrgsolution.features.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditEventRepository auditEventRepository;

    @Async
    @Override
    public void logEvent(String eventType, String userEmail, String ipAddress, String details) {
        try {
            com.pmrgsolution.constant.AuditEventType parsedType = com.pmrgsolution.constant.AuditEventType.fromString(eventType);
            if (parsedType == null) {
                parsedType = com.pmrgsolution.constant.AuditEventType.ADMIN_ACTION;
            }
            AuditEvent event = AuditEvent.builder()
                    .eventType(parsedType)
                    .userEmail(userEmail)
                    .ipAddress(ipAddress)
                    .details(details)
                    .build();
            auditEventRepository.save(event);
            log.info("Audit log recorded: [{}] user='{}', ip='{}', details='{}'", eventType, userEmail, ipAddress, details);
        } catch (Exception e) {
            log.error("Failed to record audit event: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getAuditLogs(String eventType, String userEmail, Pageable pageable) {
        Page<AuditEvent> page;
        com.pmrgsolution.constant.AuditEventType parsedType = null;
        if (eventType != null && !eventType.isBlank()) {
            parsedType = com.pmrgsolution.constant.AuditEventType.fromString(eventType);
        }

        if (parsedType != null && userEmail != null && !userEmail.isBlank()) {
            page = auditEventRepository.findByEventTypeAndUserEmailOrderByCreatedAtDesc(parsedType, userEmail.trim(), pageable);
        } else if (parsedType != null) {
            page = auditEventRepository.findByEventTypeOrderByCreatedAtDesc(parsedType, pageable);
        } else if (userEmail != null && !userEmail.isBlank()) {
            page = auditEventRepository.findByUserEmailOrderByCreatedAtDesc(userEmail.trim(), pageable);
        } else {
            page = auditEventRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::mapToResponse);
    }

    private AuditEventResponse mapToResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .userEmail(event.getUserEmail())
                .ipAddress(event.getIpAddress())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}