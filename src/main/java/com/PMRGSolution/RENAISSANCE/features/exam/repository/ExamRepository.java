package com.PMRGSolution.RENAISSANCE.features.exam.repository;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {
    
    List<Exam> findByActiveTrue();
    
    // The main fetch used in CatalogServiceImpl
    List<Exam> findByCategoryId(UUID categoryId);
    
    List<Exam> findByActiveTrueAndPublishedTrue();

    @Query("SELECT e FROM Exam e WHERE e.category.id = :catId " + 
           "AND e.active = true " +
           "AND e.published = true")
    List<Exam> findPublishedExamsByCategory(@Param("catId") UUID categoryId);
}