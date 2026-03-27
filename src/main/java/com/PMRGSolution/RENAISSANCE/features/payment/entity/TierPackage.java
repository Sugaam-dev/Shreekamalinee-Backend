package com.PMRGSolution.RENAISSANCE.features.payment.entity;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tier_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExamCategory category; // Links to NID, UCEED, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_type", nullable = false)
    private TierType tierType; // STARTER, STANDARD, PROFESSIONAL

    @Column(nullable = false)
    private Double price; // The amount to charge (e.g., 499.0)

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths; // How long the access lasts (e.g., 1, 3, 12)

    @Builder.Default
    @Column(name = "is_active")
    private boolean active = true;
}