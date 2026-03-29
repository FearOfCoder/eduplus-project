package com.example.doan10diem.repository;

import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.Enrollment;
import com.example.doan10diem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByCourseAndStudent(Course course, User student);

    List<Enrollment> findByStudent(User student);

    List<Enrollment> findByCourse(Course course);
}

