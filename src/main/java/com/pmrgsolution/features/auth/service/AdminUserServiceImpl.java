package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.exception.BusinessException;
import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.features.address.repository.ShippingAddressRepository;
import com.pmrgsolution.features.auth.dto.AdminUserDTO;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.ActiveSessionRepository;
import com.pmrgsolution.features.auth.repository.ForgotPasswordRepository;
import com.pmrgsolution.features.auth.repository.UserEmailOtpRepository;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.cart.repository.CartRepository;
import com.pmrgsolution.features.catalog.repository.ReviewRepository;
import com.pmrgsolution.features.coupon.repository.CouponUsageRepository;
import com.pmrgsolution.features.order.repository.OrderRepository;
import com.pmrgsolution.features.payment.repository.TransactionRepository;
import com.pmrgsolution.features.wishlist.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final UserEmailOtpRepository userEmailOtpRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ReviewRepository reviewRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final CartRepository cartRepository;
    private final CouponUsageRepository couponUsageRepository;
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

        if (!enabled && (user.getRole() == com.pmrgsolution.constant.Role.SUPERADMIN || user.getRole() == com.pmrgsolution.constant.Role.ADMIN)) {
            throw new BusinessException("Cannot suspend an administrator account.", HttpStatus.FORBIDDEN);
        }

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

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Prevent deleting Superadmin or Admin accounts
        if (user.getRole() == com.pmrgsolution.constant.Role.SUPERADMIN || user.getRole() == com.pmrgsolution.constant.Role.ADMIN) {
            throw new BusinessException("Cannot delete administrator account.", HttpStatus.FORBIDDEN);
        }

        // 2. Validate that user has 0 orders
        long orderCount = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
        if (orderCount > 0) {
            throw new BusinessException(
                "Cannot delete user account. This patron has " + orderCount + " active/historical order(s). Please cancel/purge their orders first before deleting this account.",
                HttpStatus.CONFLICT
            );
        }

        // 3. Validate that user has 0 transactions
        long txCount = transactionRepository.countByUserId(userId);
        if (txCount > 0) {
            throw new BusinessException(
                "Cannot delete user account. This patron has " + txCount + " historical payment transaction(s). Please remove their payment records first before deleting.",
                HttpStatus.CONFLICT
            );
        }

        // 4. Safely clean up all safe child records
        activeSessionRepository.deleteByUser(user);
        userEmailOtpRepository.deleteByUser(user);
        forgotPasswordRepository.deleteByUser(user);
        wishlistItemRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        shippingAddressRepository.deleteByUserId(userId);
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
        couponUsageRepository.deleteByUserId(userId);

        // 5. Delete the user
        userRepository.delete(user);
        log.info("Admin permanently deleted empty customer account: {}", user.getEmail());
        realtimeEventService.broadcast("CUSTOMER_DELETED", "{\"type\":\"CUSTOMER_DELETED\",\"userId\":\"" + userId + "\"}");
    }

    private AdminUserDTO mapToDTO(User u) {
        String firstName = u.getFirstName() != null ? u.getFirstName() : "";
        String lastName = u.getLastName() != null ? u.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        boolean isVerified = Boolean.TRUE.equals(u.getEmailVerified())
                || u.getProvider() == com.pmrgsolution.constant.AuthProvider.GOOGLE
                || u.isEnabled();

        return AdminUserDTO.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .fullName(!fullName.isBlank() ? fullName : u.getEmail().split("@")[0])
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .role(u.getRole() != null ? u.getRole().name() : com.pmrgsolution.constant.Role.USER.name())
                .provider(u.getProvider() != null ? u.getProvider().name() : "LOCAL")
                .enabled(u.isEnabled())
                .emailVerified(isVerified)
                .createdAt(u.getCreatedAt())
                .build();
    }
}
