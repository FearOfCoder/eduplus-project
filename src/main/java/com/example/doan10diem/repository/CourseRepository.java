package com.example.doan10diem.repository;

import com.example.doan10diem.model.Category;
import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.CourseStatus;
import com.example.doan10diem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    /** Top N khóa học mới nhất đã PUBLISHED (sắp xếp theo createdAt giảm dần). */
    List<Course> findTop8ByStatusOrderByCreatedAtDesc(CourseStatus status);

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByTeacher(User teacher);

    List<Course> findByStatusIn(List<CourseStatus> statuses);

    List<Course> findByStatusAndCategoryIn(CourseStatus status, List<Category> categories);

    List<Course> findByStatusAndCategory(CourseStatus status, Category category);

    long countByCategory(Category category);

    long countByStatus(CourseStatus status);
}

