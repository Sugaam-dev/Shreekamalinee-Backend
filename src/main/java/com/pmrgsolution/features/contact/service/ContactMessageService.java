package com.pmrgsolution.features.contact.service;

import com.pmrgsolution.features.contact.dto.ContactMessageRequest;
import com.pmrgsolution.features.contact.dto.ContactMessageResponse;

import java.util.List;
import java.util.UUID;

public interface ContactMessageService {
    ContactMessageResponse submitMessage(ContactMessageRequest request);
    List<ContactMessageResponse> getAllMessages(String statusFilter);
    ContactMessageResponse updateMessageStatus(UUID id, String status);
    void deleteMessage(UUID id);
}
