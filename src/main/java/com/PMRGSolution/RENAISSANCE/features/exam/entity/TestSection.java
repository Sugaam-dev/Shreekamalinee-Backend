package com.PMRGSolution.RENAISSANCE.features.exam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestSection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "section_name", nullable = false)
    private String sectionName; // e.g., GAT, CAT, Part A

    @Column(name = "section_description", columnDefinition = "TEXT")
    private String sectionDescription; // Added for specific section instructions

    @Column(name = "section_order")
    private Integer sectionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Question> questions = new ArrayList<>();
}