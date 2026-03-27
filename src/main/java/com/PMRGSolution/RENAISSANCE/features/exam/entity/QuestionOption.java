package com.PMRGSolution.RENAISSANCE.features.exam.entity;

import com.fasterxml.jackson.annotation.JsonProperty; // Add this import
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    @JsonProperty("isCorrect") 
    private boolean isCorrect = false;

    // Ensure this field exists and is spelled exactly like this:
    @Column(name = "option_order")
    private Integer optionOrder; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    public boolean isCorrect() { return isCorrect; }
}