package com.pmrgsolution.features.audit.dto;

import com.pmrgsolution.constant.AuditEventType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventResponse {
    private UUID id;
    private AuditEventType eventType;
    private String userEmail;
    private String ipAddress;
    private String details;
    private LocalDateTime createdAt;
}