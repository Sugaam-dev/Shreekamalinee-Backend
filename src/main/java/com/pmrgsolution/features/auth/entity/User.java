package com.pmrgsolution.features.auth.entity;

import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;

import com.pmrgsolution.constant.AuthProvider;
import com.pmrgsolution.constant.Role;
import com.pmrgsolution.features.payment.entity.Transaction;
import com.pmrgsolution.features.order.entity.Order;
import com.pmrgsolution.features.address.entity.ShippingAddress;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_phone", columnList = "phone_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) 
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName; 
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName; 

    @Column(name = "email", nullable = false, unique = true, updatable = false, length = 255)
    private String email;
    
    @Column(name = "phone_number", nullable = true, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password", length = 255)
    private String password; 

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role; 

    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean enabled = false;

    @Builder.Default
    @Column(name = "is_account_non_locked")
    private Boolean accountNonLocked = true;

    @Builder.Default
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider; // Add this field
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 

    public boolean isAccountLocked() {
        if (Boolean.FALSE.equals(this.accountNonLocked)) {
            return true;
        }
        if (this.lockoutUntil != null) {
            if (this.lockoutUntil.isAfter(LocalDateTime.now())) {
                return true;
            } else {
                // Lockout period expired
                this.lockoutUntil = null;
                this.failedLoginAttempts = 0;
                return false;
            }
        }
        return false;
    }

    public void recordFailedLogin() {
        if (this.failedLoginAttempts == null) {
            this.failedLoginAttempts = 0;
        }
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockoutUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    public void resetFailedLogin() {
        this.failedLoginAttempts = 0;
        this.lockoutUntil = null;
    }

    public int getFailedLoginAttempts() {
        return this.failedLoginAttempts != null ? this.failedLoginAttempts : 0;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    public boolean isAccountNonLocked() {
        return !Boolean.FALSE.equals(this.accountNonLocked);
    }
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActiveSession> sessions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShippingAddress> shippingAddresses;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions; // Updated name

    // NEW: Relationships for security tracking
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserEmailOtp> emailOtps;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ForgotPassword> forgotPasswordRequests;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); 
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "");
    }
}