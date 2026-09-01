package com.pmrgsolution.features.audit.dto;

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
    private String eventType;
    private String userEmail;
    private String ipAddress;
    private String details;
    private LocalDateTime createdAt;
}