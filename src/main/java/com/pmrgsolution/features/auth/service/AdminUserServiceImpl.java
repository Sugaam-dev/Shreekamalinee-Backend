package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.features.auth.dto.AdminUserDTO;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.ActiveSessionRepository;
import com.pmrgsolution.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final EmailService emailService;
    private final com.pmrgsolution.core.service.RealtimeEventService realtimeEventService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserDTO> searchUsers(String query) {
        List<User> users;
        if (query != null && !query.trim().isBlank()) {
            users = userRepository.searchUsers(query.trim());
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        }

        return users.stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public AdminUserDTO updateUserStatus(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(enabled);
        if (!enabled) {
            // If user is deactivated/blocked, immediately revoke all their active login sessions!
            activeSessionRepository.deleteByUser(user);
            log.info("User '{}' has been DEACTIVATED/BLOCKED by Admin and active sessions revoked.", user.getEmail());
        } else {
            log.info("User '{}' has been ACTIVATED by Admin.", user.getEmail());
        }

        User saved = userRepository.save(user);
        realtimeEventService.broadcast("CUSTOMER_UPDATED", "{\"type\":\"CUSTOMER_UPDATED\",\"userId\":\"" + userId + "\"}");

        // Dispatch status alert email
        try {
            if (saved.getEmail() != null) {
                emailService.sendAccountStatusChangedAlert(saved.getEmail(), saved.getFirstName(), enabled);
            }
        } catch (Exception e) {
            log.error("Failed to send account status alert email to {}: {}", saved.getEmail(), e.getMessage());
        }

        return mapToDTO(saved);
    }

    private AdminUserDTO mapToDTO(User u) {
        String firstName = u.getFirstName() != null ? u.getFirstName() : "";
        String lastName = u.getLastName() != null ? u.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();

        return AdminUserDTO.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .fullName(!fullName.isBlank() ? fullName : u.getEmail().split("@")[0])
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .role(u.getRole() != null ? u.getRole().name() : com.pmrgsolution.constant.Role.USER.name())
                .enabled(u.isEnabled())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
