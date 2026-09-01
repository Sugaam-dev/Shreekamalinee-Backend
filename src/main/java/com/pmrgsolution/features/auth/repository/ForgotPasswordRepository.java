package com.pmrgsolution.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pmrgsolution.features.auth.entity.ForgotPassword;
import com.pmrgsolution.features.auth.entity.User;

@Repository
public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, UUID> {
    
    // Finds the latest unused password reset OTP for a specific user
    Optional<ForgotPassword> findByUserAndOtpAndIsUsedFalse(User user, String otp);
}