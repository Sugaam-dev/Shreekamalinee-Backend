package com.pmrgsolution.features.catalog.repository;

import com.pmrgsolution.features.catalog.entity.Category;
import com.pmrgsolution.features.catalog.entity.Product;
import com.pmrgsolution.features.catalog.entity.ProductVariant;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductSpecifications {

    public static Specification<Product> withFilters(
            UUID categoryId,
            String gender,
            String brand,
            String search,
            String season,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Category filter (includes subcategories / recursive logic)
            if (categoryId != null) {
                Join<Product, Category> category = root.join("category", JoinType.LEFT);
                Join<Category, Category> parentCategory = category.join("parentCategory", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.equal(category.get("id"), categoryId),
                    cb.equal(parentCategory.get("id"), categoryId)
                ));
            }

            // 2. Gender filter
            if (gender != null && !gender.isBlank()) {
                com.pmrgsolution.constant.GenderCategory gc = com.pmrgsolution.constant.GenderCategory.fromString(gender);
                if (gc != null) {
                    predicates.add(cb.equal(root.get("genderCategory"), gc));
                }
            }

            // 3. Brand filter
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            }

            // 4. Season filter
            if (season != null && !season.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("season")), season.trim().toLowerCase()));
            }

            // 5. Search — ILIKE on name, description, brand, sku
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("brand")), pattern),
                    cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }

            // 6. Price filters (use offerPrice if present and > 0, else originalPrice)
            Expression<BigDecimal> effectivePrice = cb.<BigDecimal>selectCase()
                .when(cb.and(cb.isNotNull(root.get("offerPrice")), cb.greaterThan(root.get("offerPrice"), BigDecimal.ZERO)), root.get("offerPrice"))
                .otherwise(root.get("originalPrice"));

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(effectivePrice, minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(effectivePrice, maxPrice));
            }

            // 7. Stock filter — subquery on ProductVariant
            if (Boolean.TRUE.equals(inStock)) {
                Subquery<Long> stockSubquery = query.subquery(Long.class);
                Root<ProductVariant> variantRoot = stockSubquery.from(ProductVariant.class);
                stockSubquery.select(cb.count(variantRoot))
                    .where(cb.and(
                        cb.equal(variantRoot.get("product"), root),
                        cb.greaterThan(variantRoot.get("stockQuantity"), 0)
                    ));
                predicates.add(cb.greaterThan(stockSubquery, 0L));
            } else if (Boolean.FALSE.equals(inStock)) {
                Subquery<Long> stockSubquery = query.subquery(Long.class);
                Root<ProductVariant> variantRoot = stockSubquery.from(ProductVariant.class);
                stockSubquery.select(cb.count(variantRoot))
                    .where(cb.and(
                        cb.equal(variantRoot.get("product"), root),
                        cb.greaterThan(variantRoot.get("stockQuantity"), 0)
                    ));
                predicates.add(cb.equal(stockSubquery, 0L));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
