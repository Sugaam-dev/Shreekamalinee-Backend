package com.PMRGSolution.RENAISSANCE.features.auth.entity;

import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;

import com.PMRGSolution.RENAISSANCE.Constant.AuthProvider;
import com.PMRGSolution.RENAISSANCE.Constant.Role;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.Transaction;
import com.PMRGSolution.RENAISSANCE.features.payment.entity.UserSubscription;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
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

    @Column(name = "first_name", nullable = false)
    private String firstName; 
    
    @Column(name = "last_name", nullable = false)
    private String lastName; 

    @Column(name = "email", nullable = false, unique = true, updatable = false)
    private String email;
    
    @Column(name = "phone_No", nullable = true, unique = true)
    private String phoneNumber;

    @Column(name = "password", nullable = true)
    private String password; 

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role; 

    @Builder.Default
    @Column(name = "is_enabled")
    private boolean enabled = false;


    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider; // Add this field
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActiveSession> sessions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSubscription> subscriptions; // Updated name

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
}