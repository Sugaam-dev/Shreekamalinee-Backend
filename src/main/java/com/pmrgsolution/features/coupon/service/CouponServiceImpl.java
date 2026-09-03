package com.pmrgsolution.features.coupon.service;

import com.pmrgsolution.Exception.BusinessException;
import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.coupon.dto.CouponRequest;
import com.pmrgsolution.features.coupon.dto.CouponResponse;
import com.pmrgsolution.features.coupon.dto.CouponValidationResponse;
import com.pmrgsolution.features.coupon.entity.Coupon;
import com.pmrgsolution.features.coupon.repository.CouponRepository;
import com.pmrgsolution.features.coupon.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.pmrgsolution.features.coupon.dto.CouponUsageResponse;
import com.pmrgsolution.features.order.repository.OrderRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void createCoupon(CouponRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new BusinessException("Coupon code cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (couponRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new BusinessException("Coupon code already exists", HttpStatus.CONFLICT);
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .expiryDate(request.getExpiryDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .applicableUserEmails(request.getApplicableUserEmails())
                .build();

        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found");
        }
        couponRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getCouponUsages(UUID couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new ResourceNotFoundException("Coupon not found");
        }
        List<com.pmrgsolution.features.coupon.entity.CouponUsage> usages =
                couponUsageRepository.findByCouponIdOrderByUsedAtDesc(couponId);

        // PERF FIX: Batch-load all order numbers in a single query instead of N separate queries.
        // Previously each usage triggered a separate orderRepository.findById() call.
        Set<UUID> orderIds = usages.stream()
                .filter(u -> u.getOrderId() != null)
                .map(com.pmrgsolution.features.coupon.entity.CouponUsage::getOrderId)
                .collect(Collectors.toSet());

        Map<UUID, String> orderNumberMap = orderIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : orderRepository.findAllById(orderIds).stream()
                    .collect(Collectors.toMap(
                            com.pmrgsolution.features.order.entity.Order::getId,
                            com.pmrgsolution.features.order.entity.Order::getOrderNumber));

        return usages.stream()
                .map(u -> {
                    String orderNum = u.getOrderId() != null ? orderNumberMap.get(u.getOrderId()) : null;
                    return CouponUsageResponse.builder()
                            .id(u.getId())
                            .userId(u.getUser() != null ? u.getUser().getId() : null)
                            .userFullName(u.getUser() != null ? u.getUser().getFullName() : "Guest Patron")
                            .userEmail(u.getUser() != null ? u.getUser().getEmail() : "N/A")
                            .userPhone(u.getUser() != null ? u.getUser().getPhoneNumber() : "N/A")
                            .orderId(u.getOrderId())
                            .orderNumber(orderNum)
                            .usedAt(u.getUsedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal subtotal, UUID userId) {
        return validateCoupon(code, subtotal, userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal subtotal, UUID userId, String userEmail) {
        if (code == null || code.trim().isEmpty()) {
            return CouponValidationResponse.builder().valid(false).message("Coupon code is required").build();
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (coupon == null || !coupon.isActive()) {
            return CouponValidationResponse.builder().valid(false).message("Invalid or inactive coupon code").build();
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            return CouponValidationResponse.builder().valid(false).message("This coupon has expired").build();
        }

        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            return CouponValidationResponse.builder().valid(false).message("Coupon usage limit exceeded").build();
        }

        // Resolve effective user ID & email
        UUID effectiveUserId = userId;
        String effectiveEmail = userEmail != null && !userEmail.isBlank() ? userEmail.trim() : null;

        if (effectiveUserId == null && effectiveEmail != null) {
            User foundUser = userRepository.findByEmailIgnoreCase(effectiveEmail).orElse(null);
            if (foundUser != null) {
                effectiveUserId = foundUser.getId();
                effectiveEmail = foundUser.getEmail();
            }
        } else if (effectiveUserId != null && effectiveEmail == null) {
            effectiveEmail = userRepository.findById(effectiveUserId).map(User::getEmail).orElse(null);
        }

        // Check if customer has already used this single-use / restricted coupon
        if (effectiveUserId != null && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), effectiveUserId) > 0) {
            return CouponValidationResponse.builder().valid(false).message("This customer has already used this coupon").build();
        }

        // Check VIP / Restricted User Emails
        if (coupon.getApplicableUserEmails() != null && !coupon.getApplicableUserEmails().isEmpty()) {
            if (effectiveEmail == null || effectiveEmail.isBlank()) {
                return CouponValidationResponse.builder().valid(false).message("Please provide customer email to apply this VIP exclusive coupon").build();
            }
            final String checkEmail = effectiveEmail.trim().toLowerCase();
            boolean isAllowed = coupon.getApplicableUserEmails().stream()
                    .anyMatch(e -> e != null && e.trim().equalsIgnoreCase(checkEmail));
            if (!isAllowed) {
                return CouponValidationResponse.builder().valid(false).message("This exclusive coupon is not applicable to customer email: " + effectiveEmail).build();
            }
        }

        if (subtotal != null && coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("Minimum cart order amount for this coupon is ₹" + coupon.getMinOrderAmount())
                    .build();
        }

        BigDecimal calculatedDiscount = BigDecimal.ZERO;
        if (subtotal != null) {
            if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
                calculatedDiscount = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                if (coupon.getMaxDiscountAmount() != null && calculatedDiscount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    calculatedDiscount = coupon.getMaxDiscountAmount();
                }
            } else {
                calculatedDiscount = coupon.getDiscountValue();
            }
        }

        BigDecimal finalPrice = subtotal != null ? subtotal.subtract(calculatedDiscount).max(BigDecimal.ZERO) : BigDecimal.ZERO;

        return CouponValidationResponse.builder()
                .valid(true)
                .message("Coupon applied successfully!")
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .calculatedDiscount(calculatedDiscount)
                .discountAmount(calculatedDiscount)
                .finalPrice(finalPrice)
                .build();
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .minPurchaseAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .timesUsed(coupon.getTimesUsed())
                .expiryDate(coupon.getExpiryDate())
                .isActive(coupon.isActive())
                .active(coupon.isActive())
                .applicableUserEmails(coupon.getApplicableUserEmails())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}