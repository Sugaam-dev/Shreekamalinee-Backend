package com.pmrgsolution.features.contact.service;

import com.pmrgsolution.constant.ContactMessageStatus;
import com.pmrgsolution.exception.BusinessException;
import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.features.contact.dto.ContactMessageRequest;
import com.pmrgsolution.features.contact.dto.ContactMessageResponse;
import com.pmrgsolution.features.contact.entity.ContactMessage;
import com.pmrgsolution.features.contact.repository.ContactMessageRepository;
import com.pmrgsolution.features.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;
    private final com.pmrgsolution.core.service.RealtimeEventService realtimeEventService;

    @Value("${app.admin.notification-email:${app.admin.email:admin@shreekamalinee.com}}")
    private String adminEmail;

    @Override
    @Transactional
    public ContactMessageResponse submitMessage(ContactMessageRequest request) {
        ContactMessage message = ContactMessage.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .subject(request.getSubject() != null ? request.getSubject().trim() : "General Inquiry")
                .message(request.getMessage().trim())
                .status(ContactMessageStatus.NEW)
                .build();

        ContactMessage saved = contactMessageRepository.save(message);
        log.info("Received new customer contact inquiry from '{}' <{}> with subject '{}'", saved.getName(), saved.getEmail(), saved.getSubject());
        realtimeEventService.broadcast("INQUIRY_UPDATED", "{\"type\":\"INQUIRY_UPDATED\",\"id\":\"" + saved.getId() + "\"}");

        try {
            if (adminEmail != null && !adminEmail.isBlank()) {
                emailService.sendAdminNewInquiryAlert(
                    adminEmail,
                    saved.getName(),
                    saved.getEmail(),
                    saved.getPhone(),
                    saved.getSubject(),
                    saved.getMessage()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch admin contact inquiry email alert: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactMessageResponse> getAllMessages(String statusFilter) {
        List<ContactMessage> messages;
        ContactMessageStatus parsedStatus = (statusFilter != null && !statusFilter.equalsIgnoreCase("all") && !statusFilter.isBlank())
                ? ContactMessageStatus.fromString(statusFilter)
                : null;

        if (parsedStatus != null) {
            messages = contactMessageRepository.findByStatusOrderByCreatedAtDesc(parsedStatus);
        } else {
            messages = contactMessageRepository.findAllByOrderByCreatedAtDesc();
        }
        return messages.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public ContactMessageResponse updateMessageStatus(UUID id, String status) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact inquiry not found with id: " + id));

        ContactMessageStatus newStatus = ContactMessageStatus.fromString(status);
        if (newStatus == null) {
            throw new BusinessException("Invalid message status: " + status, HttpStatus.BAD_REQUEST);
        }

        message.setStatus(newStatus);
        ContactMessage saved = contactMessageRepository.save(message);
        realtimeEventService.broadcast("INQUIRY_UPDATED", "{\"type\":\"INQUIRY_UPDATED\",\"id\":\"" + id + "\"}");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMessage(UUID id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Contact inquiry not found with id: " + id);
        }
        contactMessageRepository.deleteById(id);
        realtimeEventService.broadcast("INQUIRY_UPDATED", "{\"type\":\"INQUIRY_UPDATED\",\"id\":\"" + id + "\"}");
    }

    private ContactMessageResponse mapToResponse(ContactMessage entity) {
        return ContactMessageResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .subject(entity.getSubject())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
