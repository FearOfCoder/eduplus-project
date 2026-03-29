package com.example.doan10diem.repository;

import com.example.doan10diem.model.Assignment;
import com.example.doan10diem.model.Submission;
import com.example.doan10diem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);

    List<Submission> findByStudent(User student);

    /** Lấy tất cả bài nộp của một bài tập (để filter/sort ở tầng service hoặc controller). */
    List<Submission> findByAssignment(Assignment assignment);
}

