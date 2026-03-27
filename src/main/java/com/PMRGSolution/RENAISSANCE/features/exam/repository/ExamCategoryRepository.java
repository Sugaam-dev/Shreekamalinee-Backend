package com.PMRGSolution.RENAISSANCE.features.exam.repository;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.ExamCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamCategoryRepository extends JpaRepository<ExamCategory, UUID> {
}