package com.pmrgsolution.features.contact.controller;

import com.pmrgsolution.features.contact.dto.ContactMessageRequest;
import com.pmrgsolution.features.contact.dto.ContactMessageResponse;
import com.pmrgsolution.features.contact.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactMessageService contactMessageService;

    @PostMapping
    public ResponseEntity<ContactMessageResponse> submitContactForm(@Valid @RequestBody ContactMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contactMessageService.submitMessage(request));
    }
}
