package com.pmrgsolution.features.auth.entity;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_email_otp")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_otp", length = 10, nullable = false)
    private String emailOtp;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Builder.Default
    @Column(name = "is_used")
    private Boolean isUsed = false;

    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public boolean isUsed() {
        return Boolean.TRUE.equals(this.isUsed);
    }

    public void setUsed(boolean used) {
        this.isUsed = used;
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(this.isVerified);
    }

    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }
}