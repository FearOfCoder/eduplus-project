package com.example.doan10diem.repository;

import com.example.doan10diem.dto.CourseRevenueDTO;
import com.example.doan10diem.model.Payment;
import com.example.doan10diem.model.PaymentStatus;
import com.example.doan10diem.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStudentOrderByCreatedAtDesc(User student);

    @EntityGraph(attributePaths = {"course", "course.teacher"})
    List<Payment> findByStatusOrderByCreatedAtDesc(com.example.doan10diem.model.PaymentStatus status);

    List<Payment> findByCourseTeacherAndStatus(User teacher, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT new com.example.doan10diem.dto.CourseRevenueDTO(p.course.title, COUNT(p), " +
           "COALESCE(SUM(p.amount), 0), COALESCE(SUM(p.teacherShare), 0)) " +
           "FROM Payment p WHERE p.course.teacher = :teacher AND p.status = :status " +
           "GROUP BY p.course.id, p.course.title ORDER BY SUM(p.teacherShare) DESC")
    List<CourseRevenueDTO> findCourseRevenueByTeacherAndStatus(@Param("teacher") User teacher, @Param("status") PaymentStatus status);
}

