package com.PMRGSolution.RENAISSANCE.features.catalog.entity;

import java.time.LocalDateTime;

import java.util.UUID;

import com.PMRGSolution.RENAISSANCE.Constant.ContentTypes;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExamCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentTypes contentType;

    // NEW: The "Access Gate"
    @Enumerated(EnumType.STRING)
    private TierType requiredTier; 

    // NEW: Used to limit Mock Tests (e.g., 1, 2, 3... 12)
    private Integer displayOrder; 

    @Lob
    @Column(name = "content", columnDefinition = "LONGBLOB") 
    private byte[] content; 

    @Builder.Default
    private boolean protectedFile = true;

    private String s3Url;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}