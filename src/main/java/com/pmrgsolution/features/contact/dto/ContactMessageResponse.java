package com.pmrgsolution.features.contact.dto;

import com.pmrgsolution.constant.ContactMessageStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private ContactMessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}