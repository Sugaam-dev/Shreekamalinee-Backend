package com.PMRGSolution.RENAISSANCE.features.auth.service;

import com.PMRGSolution.RENAISSANCE.Exception.ResourceNotFoundException;
import com.PMRGSolution.RENAISSANCE.features.auth.dto.UserAccountDTO;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.UserRepository;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.UserSubscription;
import com.PMRGSolution.RENAISSANCE.features.payment.repository.UserSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserAccountDTO getUserAccountDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Fetch subscriptions active within a buffer
        List<UserSubscription> subscriptions = subscriptionRepository.findAllActiveSubscriptions(
                user.getEmail(), LocalDateTime.now().minusDays(2));

        List<UserAccountDTO.SubscriptionBrief> subDTOs = subscriptions.stream()
                .map(this::mapToSubscriptionBrief)
                .collect(Collectors.toList());

        return UserAccountDTO.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .isEmailVerified(user.isEnabled())
                .activeSubscriptions(subDTOs)
                .build();
    }

    @Override
    @Transactional
    public void updateProfile(UUID userId, UserAccountDTO updateRequest) {
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
        if (updateRequest.getPhone() != null) {
            user.setPhoneNumber(updateRequest.getPhone().trim());
        }
        
        userRepository.saveAndFlush(user); 
        log.info("Profile successfully updated for: {}", user.getEmail());
    }

   private UserAccountDTO.SubscriptionBrief mapToSubscriptionBrief(UserSubscription sub) {
    // Current time for comparison
    LocalDateTime now = LocalDateTime.now();
    long daysRemaining = ChronoUnit.DAYS.between(now, sub.getExpiryDate());
    boolean isExpired = now.isAfter(sub.getExpiryDate());
    
    return UserAccountDTO.SubscriptionBrief.builder()
            .categoryId(sub.getCategory().getId())
            .categoryName(sub.getCategory().getDisplayName()) // <-- FAST: Category already fetched!
            .tier(sub.getTier())
            .expiryDate(sub.getExpiryDate())
            .daysRemaining(Math.max(0, daysRemaining))
            .isExpired(isExpired)
            .build();
}
}