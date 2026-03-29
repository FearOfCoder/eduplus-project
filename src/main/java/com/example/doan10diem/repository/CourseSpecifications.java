package com.example.doan10diem.repository;

import com.example.doan10diem.model.Category;
import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.CourseStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.List;

public final class CourseSpecifications {

    private CourseSpecifications() {}

    public static Specification<Course> statusIs(CourseStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Course> keywordInTitleOrDescription(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    public static Specification<Course> categoryIn(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("category").in(categories);
    }

    public static Specification<Course> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();
            if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) >= 0) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return p;
        };
    }
}
