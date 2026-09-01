package com.pmrgsolution.features.contact.controller;

import com.pmrgsolution.features.contact.dto.ContactMessageResponse;
import com.pmrgsolution.features.contact.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/contact")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@RequiredArgsConstructor
public class AdminContactController {

    private final ContactMessageService contactMessageService;

    @GetMapping
    public ResponseEntity<List<ContactMessageResponse>> getAllMessages(
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(contactMessageService.getAllMessages(status));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ContactMessageResponse> updateMessageStatus(
            @PathVariable UUID id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(contactMessageService.updateMessageStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID id) {
        contactMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
}
