package com.PMRGSolution.RENAISSANCE.features.exam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "category_code", unique = true, nullable = false)
    private String categoryCode; 

    @Column(name = "display_name", nullable = false)
    private String displayName; 

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}