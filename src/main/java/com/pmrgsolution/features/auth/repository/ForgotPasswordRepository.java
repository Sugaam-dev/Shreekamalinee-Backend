package com.pmrgsolution.features.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pmrgsolution.features.auth.entity.ForgotPassword;
import com.pmrgsolution.features.auth.entity.User;

@Repository
public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, UUID> {
    
    Optional<ForgotPassword> findByUserAndOtpAndIsUsedFalse(User user, String otp);

    @Modifying
    @Query("DELETE FROM ForgotPassword f WHERE f.expiresAt < :now OR f.isUsed = true")
    int deleteExpiredOrUsed(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM ForgotPassword f WHERE f.user = :user")
    void deleteByUser(@Param("user") User user);
}