package com.PMRGSolution.RENAISSANCE.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.PMRGSolution.RENAISSANCE.features.auth.entity.ActiveSession;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {
    
    Optional<ActiveSession> findBySessionId(UUID sessionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ActiveSession s WHERE s.user = :user")
    void deleteByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM ActiveSession s WHERE s.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") UUID sessionId);
}