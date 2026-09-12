package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.features.auth.dto.UserAccountDTO;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserAccountDTO getUserAccountDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserAccountDTO.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .isEmailVerified(user.isEnabled())
                .build();
    }

    @Override
    @Transactional
    public UserAccountDTO updateProfile(UUID userId, UserAccountDTO updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 🛡️ SECURITY: Explicitly update ONLY safe fields.
        // This prevents accidental password or role overwrites.
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName().trim());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName().trim());
        }
        if (updateRequest.getPhone() != null && !updateRequest.getPhone().isBlank()) {
            String newPhone = updateRequest.getPhone().trim();
            if (!newPhone.equals(user.getPhoneNumber())) {
                if (userRepository.existsByPhoneNumber(newPhone)) {
                    throw new com.pmrgsolution.exception.BusinessException(
                            "This mobile number is already linked to another account. Please use a different number.",
                            org.springframework.http.HttpStatus.CONFLICT);
                }
                user.setPhoneNumber(newPhone);
            }
        }
        
        userRepository.saveAndFlush(user); 
        log.info("Profile successfully updated for: {}", user.getEmail());
        return getUserAccountDetails(userId);
    }
}