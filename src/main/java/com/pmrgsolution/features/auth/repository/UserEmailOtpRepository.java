package com.pmrgsolution.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.entity.UserEmailOtp;

@Repository
public interface UserEmailOtpRepository extends JpaRepository<UserEmailOtp, UUID> {
    
    // Finds the latest unused OTP for a specific user
    Optional<UserEmailOtp> findByUserAndEmailOtpAndIsUsedFalse(User user, String emailOtp);
}