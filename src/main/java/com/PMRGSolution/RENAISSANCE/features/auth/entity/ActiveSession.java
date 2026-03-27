package com.PMRGSolution.RENAISSANCE.features.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "active_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Internal Database Primary Key

    @Column(name = "session_id", unique = true, nullable = false)
    private UUID sessionId; // Public identifier used in JWT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String deviceId;
    private boolean deviceType; // true for mobile, false for web
    private LocalDateTime loginTime;
    private LocalDateTime lastActive;

    @PrePersist
    protected void onCreate() {
        if (this.sessionId == null) {
            this.sessionId = UUID.randomUUID();
        }
    }
}