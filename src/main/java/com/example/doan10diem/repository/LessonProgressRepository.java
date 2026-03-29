package com.example.doan10diem.repository;

import com.example.doan10diem.model.Lesson;
import com.example.doan10diem.model.LessonProgress;
import com.example.doan10diem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    /**
     * Tìm tiến độ học tập của một student cho một lesson cụ thể.
     */
    Optional<LessonProgress> findByStudentAndLesson(User student, Lesson lesson);

    /**
     * Lấy danh sách tất cả bài học đã hoàn thành của một student trong một khóa học.
     */
    List<LessonProgress> findByStudentAndLessonCourse(User student, com.example.doan10diem.model.Course course);

    /**
     * Đếm số bài học đã hoàn thành của một student trong một khóa học.
     */
    long countByStudentAndLessonCourseAndIsCompletedTrue(User student, com.example.doan10diem.model.Course course);
}
