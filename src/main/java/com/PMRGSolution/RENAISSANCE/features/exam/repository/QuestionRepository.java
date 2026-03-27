package com.PMRGSolution.RENAISSANCE.features.exam.repository;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    // For the Admin to see all questions in a specific Mock Test
    @Query("SELECT q FROM Question q WHERE q.section.exam.id = :examId")
    List<Question> findByExamId(@Param("examId") UUID examId);

    // For the "Single Add/Update" feature within a specific Section (Part A or B)
    List<Question> findBySectionId(UUID sectionId);

    // Standard deleteById(UUID) is inherited, but you can add custom checks if needed
}