package com.PMRGSolution.RENAISSANCE.features.exam.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.Constant.ResultStatus; 
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "objective_score")
    @Builder.Default
    private Double objectiveScore = 0.0;

    @Column(name = "subjective_score")
    @Builder.Default
    private Double subjectiveScore = 0.0;

    @Column(name = "total_score")
    @Builder.Default
    private Double totalScore = 0.0;

    @Column(name = "total_marks_possible")
    private Double totalMarksPossible;

    @Enumerated(EnumType.STRING)
    private ResultStatus status; // IN_PROGRESS, UNDER_EVALUATION, COMPLETED

    @Column(name = "is_fully_evaluated")
    @Builder.Default
    private boolean fullyEvaluated = false;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime; // Added: Server-side absolute deadline

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat; // Added: To track user activity/connection

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "testResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserResponse> responses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = ResultStatus.IN_PROGRESS;
        if (this.startTime == null) this.startTime = LocalDateTime.now();
        // Expiry is handled in the Service layer based on Exam duration
    }
}