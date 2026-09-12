package com.pmrgsolution.features.coupon.service;

import com.pmrgsolution.constant.DiscountType;
import com.pmrgsolution.exception.BusinessException;
import com.pmrgsolution.exception.ResourceNotFoundException;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.coupon.dto.CouponRequest;
import com.pmrgsolution.features.coupon.dto.CouponResponse;
import com.pmrgsolution.features.coupon.dto.CouponUsageResponse;
import com.pmrgsolution.features.coupon.dto.CouponValidationResponse;
import com.pmrgsolution.features.coupon.entity.Coupon;
import com.pmrgsolution.features.coupon.repository.CouponRepository;
import com.pmrgsolution.features.coupon.repository.CouponUsageRepository;
import com.pmrgsolution.features.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final com.pmrgsolution.core.service.RealtimeEventService realtimeEventService;

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
                .minOrderAmount(request.getMinPurchaseAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .expiryDate(request.getExpiryDate())
                .isActive(true)
                .applicableUserEmails(request.getApplicableUserEmails())
                .build();

        Coupon saved = couponRepository.save(coupon);
        realtimeEventService.broadcast("COUPON_UPDATED", "{\"type\":\"COUPON_UPDATED\",\"code\":\"" + saved.getCode() + "\"}");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAvailableCouponsForUser(UUID userId, String userEmail) {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findAll().stream()
                .filter(c -> {
                    if (c.getApplicableUserEmails() != null && !c.getApplicableUserEmails().isEmpty()) {
                        return userEmail != null && c.getApplicableUserEmails().stream()
                                .anyMatch(email -> email != null && email.equalsIgnoreCase(userEmail));
                    }
                    return true;
                })
                .map(c -> {
                    CouponResponse resp = mapToResponse(c);
                    boolean expired = !c.isActive() || (c.getExpiryDate() != null && c.getExpiryDate().isBefore(now));
                    resp.setIsExpired(expired);
                    if (userId != null) {
                        boolean used = couponUsageRepository.countByCouponIdAndUserId(c.getId(), userId) > 0;
                        resp.setIsUsedByUser(used);
                    } else {
                        resp.setIsUsedByUser(false);
                    }
                    return resp;
                })
                .toList();
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found");
        }
        couponRepository.deleteById(id);
        realtimeEventService.broadcast("COUPON_UPDATED", "{\"type\":\"COUPON_UPDATED\",\"couponId\":\"" + id + "\"}");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getCouponUsages(UUID couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new ResourceNotFoundException("Coupon not found");
        }
        List<com.pmrgsolution.features.coupon.entity.CouponUsage> usages =
                couponUsageRepository.findByCouponIdOrderByUsedAtDesc(couponId);

        Set<UUID> orderIds = usages.stream()
                .filter(u -> u.getOrderId() != null)
                .map(com.pmrgsolution.features.coupon.entity.CouponUsage::getOrderId)
                .collect(Collectors.toSet());

        Map<UUID, String> orderNumberMap = orderIds.isEmpty()
                ? Collections.emptyMap()
                : orderRepository.findAllById(orderIds).stream()
                    .collect(Collectors.toMap(
                            com.pmrgsolution.features.order.entity.Order::getId,
                            com.pmrgsolution.features.order.entity.Order::getOrderNumber));

        return usages.stream().map(u -> CouponUsageResponse.builder()
                .id(u.getId())
                .userId(u.getUser() != null ? u.getUser().getId() : null)
                .userEmail(u.getUser() != null ? u.getUser().getEmail() : "Guest Patron")
                .userFullName(u.getUser() != null ? u.getUser().getFullName() : "Guest")
                .userPhone(u.getUser() != null ? u.getUser().getPhoneNumber() : null)
                .orderId(u.getOrderId())
                .orderNumber(u.getOrderId() != null ? orderNumberMap.getOrDefault(u.getOrderId(), "N/A") : "N/A")
                .usedAt(u.getUsedAt())
                .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal subtotal, UUID userId) {
        return validateCoupon(code, subtotal, userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal subtotal, UUID userId, String userEmail) {
        if (code == null || code.isBlank()) {
            return CouponValidationResponse.builder().valid(false).message("Coupon code cannot be blank").build();
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (coupon == null) {
            return CouponValidationResponse.builder().valid(false).message("Invalid coupon code").build();
        }

        if (!coupon.isActive()) {
            return CouponValidationResponse.builder().valid(false).message("This coupon is no longer active").build();
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            return CouponValidationResponse.builder().valid(false).message("Coupon has expired").build();
        }

        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            return CouponValidationResponse.builder().valid(false).message("Coupon usage limit has been reached").build();
        }

        UUID effectiveUserId = userId;
        String effectiveEmail = userEmail;
        if (effectiveUserId == null && effectiveEmail != null && !effectiveEmail.isBlank()) {
            User foundUser = userRepository.findByEmailIgnoreCase(effectiveEmail.trim()).orElse(null);
            if (foundUser != null) {
                effectiveUserId = foundUser.getId();
                effectiveEmail = foundUser.getEmail();
            }
        } else if (effectiveUserId != null && effectiveEmail == null) {
            effectiveEmail = userRepository.findById(effectiveUserId).map(User::getEmail).orElse(null);
        }

        if (effectiveUserId != null && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), effectiveUserId) > 0) {
            return CouponValidationResponse.builder().valid(false).message("This customer has already used this coupon").build();
        }

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
        if (subtotal != null && coupon.getDiscountType() != null) {
            calculatedDiscount = switch (coupon.getDiscountType()) {
                case PERCENTAGE -> {
                    BigDecimal disc = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    yield (coupon.getMaxDiscountAmount() != null && disc.compareTo(coupon.getMaxDiscountAmount()) > 0)
                            ? coupon.getMaxDiscountAmount() : disc;
                }
                case FIXED -> coupon.getDiscountValue();
            };
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