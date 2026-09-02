package com.pmrgsolution.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.pmrgsolution.features.auth.entity.ActiveSession;
import com.pmrgsolution.features.auth.entity.User;
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {
    
    Optional<ActiveSession> findBySessionId(UUID sessionId);
    
    Optional<ActiveSession> findByRefreshToken(String refreshToken);
    
    Optional<ActiveSession> findByPreviousRefreshToken(String previousRefreshToken);

    @Modifying
    @Transactional
    @Query("DELETE FROM ActiveSession s WHERE s.user = :user")
    void deleteByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM ActiveSession s WHERE s.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") UUID sessionId);
}