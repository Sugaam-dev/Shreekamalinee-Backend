package com.PMRGSolution.RENAISSANCE.features.exam.entity;

import java.util.List;
import java.util.UUID;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_responses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_result_id", nullable = false)
    private TestResult testResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ElementCollection
    @CollectionTable(name = "user_response_options", joinColumns = @JoinColumn(name = "response_id"))
    @Column(name = "option_id")
    private List<UUID> selectedOptionIds;

    @Column(name = "numerical_answer")
    private Double numericalAnswer;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Lob
    @Column(name = "sketch_data", length = 10000000)
    private byte[] sketchData;

    @Column(name = "sketch_url")
    private String sketchUrl; // Keep this for future S3 migration

    @Column(name = "marks_obtained")
    private Double marksObtained;

    @Column(name = "is_evaluated")
    @Builder.Default
    private boolean evaluated = false;

    // Added: To help React track "Marked for Review" or "Saved" status
    @Column(name = "status")
    @Builder.Default
    private String responseStatus = "SAVED"; 
}