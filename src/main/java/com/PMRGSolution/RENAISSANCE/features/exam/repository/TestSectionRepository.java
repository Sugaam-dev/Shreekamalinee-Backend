package com.PMRGSolution.RENAISSANCE.features.exam.repository;

import com.PMRGSolution.RENAISSANCE.features.exam.entity.TestSection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestSectionRepository extends JpaRepository<TestSection, UUID> {
    List<TestSection> findByExamIdOrderBySectionOrderAsc(java.util.UUID examId);
}