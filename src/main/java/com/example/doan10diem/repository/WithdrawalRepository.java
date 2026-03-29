package com.example.doan10diem.repository;

import com.example.doan10diem.model.User;
import com.example.doan10diem.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    @Query("SELECT COALESCE(SUM(w.amount), 0.0) FROM Withdrawal w WHERE w.teacher.id = :teacherId AND w.status IN (com.example.doan10diem.model.WithdrawalStatus.SUCCESS, com.example.doan10diem.model.WithdrawalStatus.PENDING)")
    Double getTotalWithdrawnByTeacher(@Param("teacherId") Long teacherId);

    List<Withdrawal> findByTeacherOrderByRequestDateDesc(User teacher);
}
