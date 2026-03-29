package com.example.doan10diem.repository;

import com.example.doan10diem.model.AdminWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminWithdrawalRepository extends JpaRepository<AdminWithdrawal, Long> {

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM AdminWithdrawal a WHERE a.status = 'SUCCESS'")
    Double getTotalAdminWithdrawn();

    List<AdminWithdrawal> findAllByOrderByRequestDateDesc();
}
