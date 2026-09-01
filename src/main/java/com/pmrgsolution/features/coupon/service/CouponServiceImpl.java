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
        return couponUsageRepository.findByCouponIdOrderByUsedAtDesc(couponId).stream()
                .map(u -> {
                    String orderNum = null;
                    if (u.getOrderId() != null) {
                        orderNum = orderRepository.findById(u.getOrderId())
                                .map(com.pmrgsolution.features.order.entity.Order::getOrderNumber)
                                .orElse(null);
                    }
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

        if (userId != null && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId) > 0) {
            return CouponValidationResponse.builder().valid(false).message("You have already used this coupon").build();
        }

        if (coupon.getApplicableUserEmails() != null && !coupon.getApplicableUserEmails().isEmpty()) {
            if (userId == null) {
                return CouponValidationResponse.builder().valid(false).message("Please login to apply this VIP exclusive coupon").build();
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !coupon.getApplicableUserEmails().stream().anyMatch(e -> e.equalsIgnoreCase(user.getEmail()))) {
                return CouponValidationResponse.builder().valid(false).message("This exclusive coupon is not applicable to your account").build();
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