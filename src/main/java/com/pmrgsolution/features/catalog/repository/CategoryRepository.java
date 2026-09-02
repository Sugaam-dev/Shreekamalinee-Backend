package com.pmrgsolution.features.catalog.repository;

import com.pmrgsolution.features.catalog.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);

    boolean existsBySlugAndParentCategory(String slug, Category parentCategory);

    boolean existsBySlugAndParentCategoryIsNull(String slug);

    boolean existsBySlugAndParentCategoryAndIdNot(String slug, Category parentCategory, UUID id);

    boolean existsBySlugAndParentCategoryIsNullAndIdNot(String slug, UUID id);

    @EntityGraph(attributePaths = {"suggestedAttributes", "parentCategory"})
    List<Category> findAll();

    @EntityGraph(attributePaths = {"suggestedAttributes", "parentCategory"})
    List<Category> findByParentCategoryIsNull();

    @EntityGraph(attributePaths = {"suggestedAttributes", "parentCategory"})
    List<Category> findByParentCategoryIsNotNull();

    @EntityGraph(attributePaths = {"suggestedAttributes", "parentCategory"})
    List<Category> findByParentCategoryId(UUID parentId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Category c SET c.parentCategory = null WHERE c.parentCategory.id = :parentId")
    void unlinkParentCategory(@org.springframework.data.repository.query.Param("parentId") UUID parentId);
}
