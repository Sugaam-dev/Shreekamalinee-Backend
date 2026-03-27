package com.PMRGSolution.RENAISSANCE.features.payment.entity;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.features.catalog.entity.Package; // Updated Import
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExamCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_uuid", nullable = false) // References Package.uuid
    private Package productPackage; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TierType tier;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    private boolean active = true;

    public boolean isValid() {
        return active && expiryDate.isAfter(LocalDateTime.now());
    }
}