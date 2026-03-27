package com.PMRGSolution.RENAISSANCE.features.exam.entity;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes;
import com.PMRGSolution.RENAISSANCE.Constant.TierType;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "total_marks")
    private Double totalMarks; 

    /**
     * FLEXIBLE TYPE: 
     * Plain String (e.g., "MOCK_TEST", "QUIZ", "PRACTICE").
     * No fixed Enum constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentTypes contentType;

    /**
     * THE ACCESS LOCK:
     * Stays exactly as provided by Admin in the Request Body.
     * Defaults to UNIVERSAL_FREE only if the Admin sends nothing.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_tier", nullable = false)
    @Builder.Default
    private TierType requiredTier = TierType.UNIVERSAL_FREE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExamCategory category;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestSection> sections = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_published")
    @Builder.Default
    private boolean published = false;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() { 
      
        this.createdAt = LocalDateTime.now(); 
    }
}