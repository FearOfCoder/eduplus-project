package com.example.doan10diem.repository;

import com.example.doan10diem.model.Assignment;
import com.example.doan10diem.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourse(Course course);
}

