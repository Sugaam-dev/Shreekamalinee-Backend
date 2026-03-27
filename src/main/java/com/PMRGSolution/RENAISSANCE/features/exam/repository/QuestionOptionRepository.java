package com.PMRGSolution.RENAISSANCE.features.exam.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.QuestionOption;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {
    
    // Helpful for the evaluation logic to verify correct answers
    List<QuestionOption> findByQuestionId(UUID questionId);
}