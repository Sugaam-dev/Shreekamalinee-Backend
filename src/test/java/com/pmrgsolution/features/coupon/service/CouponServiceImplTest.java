package com.pmrgsolution.features.coupon.service;

import com.pmrgsolution.constant.DiscountType;
import com.pmrgsolution.features.auth.repository.UserRepository;
import com.pmrgsolution.features.coupon.dto.CouponValidationResponse;
import com.pmrgsolution.features.coupon.entity.Coupon;
import com.pmrgsolution.features.coupon.repository.CouponRepository;
import com.pmrgsolution.features.coupon.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl Unit Tests")
class CouponServiceImplTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponUsageRepository couponUsageRepository;
    @Mock private UserRepository userRepository;
    @Mock private com.pmrgsolution.core.service.RealtimeEventService realtimeEventService;

    @InjectMocks private CouponServiceImpl couponService;

    private UUID userId;
    private Coupon percentageCoupon;
    private Coupon flatCoupon;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        percentageCoupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(500))
                .usageLimit(100)
                .timesUsed(0)
                .isActive(true)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();

        flatCoupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("FLAT100")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(100))
                .minOrderAmount(BigDecimal.valueOf(999))
                .usageLimit(50)
                .timesUsed(0)
                .isActive(true)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("validateCoupon_percentage: 20% discount on Rs.1000 = Rs.200")
    void validateCoupon_percentage_coupon() {
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentageCoupon));
        when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), userId)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("SAVE20", BigDecimal.valueOf(1000), userId);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getCalculatedDiscount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.getMessage()).contains("applied");
    }

    @Test
    @DisplayName("validateCoupon_flat: FLAT100 on Rs.1500 = Rs.100 discount")
    void validateCoupon_flat_coupon() {
        when(couponRepository.findByCodeIgnoreCase("FLAT100")).thenReturn(Optional.of(flatCoupon));
        when(couponUsageRepository.countByCouponIdAndUserId(flatCoupon.getId(), userId)).thenReturn(0L);

        CouponValidationResponse result = couponService.validateCoupon("FLAT100", BigDecimal.valueOf(1500), userId);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getCalculatedDiscount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("validateCoupon_expired: Returns invalid for expired coupon")
    void validateCoupon_expired_returns_invalid() {
        percentageCoupon.setExpiryDate(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentageCoupon));

        CouponValidationResponse result = couponService.validateCoupon("SAVE20", BigDecimal.valueOf(1000), userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).containsIgnoringCase("expired");
    }

    @Test
    @DisplayName("validateCoupon_usageLimitExceeded: Returns invalid when limit hit")
    void validateCoupon_usageLimit_exceeded_returns_invalid() {
        percentageCoupon.setTimesUsed(100);
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentageCoupon));

        CouponValidationResponse result = couponService.validateCoupon("SAVE20", BigDecimal.valueOf(1000), userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).containsIgnoringCase("limit");
    }

    @Test
    @DisplayName("validateCoupon_alreadyUsedByUser: Returns invalid if user already used this coupon")
    void validateCoupon_alreadyUsed_by_user() {
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentageCoupon));
        when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), userId)).thenReturn(1L);

        CouponValidationResponse result = couponService.validateCoupon("SAVE20", BigDecimal.valueOf(1000), userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).containsIgnoringCase("already used");
    }

    @Test
    @DisplayName("validateCoupon_minAmountNotMet: Returns invalid when order total is too low")
    void validateCoupon_minAmount_not_met() {
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentageCoupon));

        // Order total Rs.400 — minimum is Rs.500
        CouponValidationResponse result = couponService.validateCoupon("SAVE20", BigDecimal.valueOf(400), userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).containsIgnoringCase("minimum");
    }

    @Test
    @DisplayName("validateCoupon_unknownCode: Returns invalid for non-existent coupon code")
    void validateCoupon_unknown_code_returns_invalid() {
        when(couponRepository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

        CouponValidationResponse result = couponService.validateCoupon("INVALID", BigDecimal.valueOf(1000), userId);

        assertThat(result.isValid()).isFalse();
    }
}
