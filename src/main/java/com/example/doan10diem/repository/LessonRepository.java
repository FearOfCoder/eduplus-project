package com.example.doan10diem.repository;

import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseOrderBySortOrderAsc(Course course);
}

