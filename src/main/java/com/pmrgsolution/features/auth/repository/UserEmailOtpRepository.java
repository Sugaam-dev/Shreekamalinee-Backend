package com.pmrgsolution.features.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.entity.UserEmailOtp;

@Repository
public interface UserEmailOtpRepository extends JpaRepository<UserEmailOtp, UUID> {
    
    Optional<UserEmailOtp> findByUserAndEmailOtpAndIsUsedFalse(User user, String emailOtp);

    @Modifying
    @Query("DELETE FROM UserEmailOtp u WHERE u.expiresAt < :now OR u.isUsed = true")
    int deleteExpiredOrUsed(@Param("now") LocalDateTime now);
}