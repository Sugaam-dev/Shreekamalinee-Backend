package com.pmrgsolution.features.contact.repository;

import com.pmrgsolution.constant.ContactMessageStatus;
import com.pmrgsolution.features.contact.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactMessageStatus status);
}
