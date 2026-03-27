package com.PMRGSolution.RENAISSANCE.features.exam.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.PMRGSolution.RENAISSANCE.Constant.QuestionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; 

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType type; // MCQ, MSQ (Multiple Correct), NAT (Numerical), SUBJECTIVE

    @Column(nullable = false)
    private Double marks;

    @Column(name = "negative_marks")
    @Builder.Default
    private Double negativeMarks = 0.0;

    @Column(name = "correct_numerical_answer")
    private Double correctNumericalAnswer; // Added for NAT questions

    @Column(name = "question_order")
    private Integer questionOrder;

    @Column(columnDefinition = "TEXT")
    private String explanation; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private TestSection section;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserResponse> userResponses = new ArrayList<>();
}