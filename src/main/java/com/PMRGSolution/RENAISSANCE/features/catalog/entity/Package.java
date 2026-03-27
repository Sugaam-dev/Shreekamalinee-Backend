package com.PMRGSolution.RENAISSANCE.features.catalog.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.PMRGSolution.RENAISSANCE.Constant.TierType; // Ensure this matches diagram strings
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "packages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private UUID uuid;

    @Column(name = "name", nullable = false)
    private String name;
    private Integer mockTestLimit;// e.g., 0 for Starter, 3 for Standard, 12 for Professional
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_type", nullable = false)
    private TierType tierType; // UNIVERSAL_FREE, STARTER, STANDARD, PROFESSIONAL

    @Column(name = "price_amount", nullable = false)
    private BigDecimal priceAmount;

    @Column(name = "duration_in_months", nullable = false)
    private int durationInMonths;

    // Added to support the "Access Bridge" logic
    @ManyToMany
    @JoinTable(
        name = "package_category_access",
        joinColumns = @JoinColumn(name = "package_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<ExamCategory> accessibleCategories;
}