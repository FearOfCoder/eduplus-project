package com.example.doan10diem.repository;

import com.example.doan10diem.model.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Long> {

    @Query("SELECT DISTINCT c FROM FaqCategory c LEFT JOIN FETCH c.faqs ORDER BY c.id ASC")
    List<FaqCategory> findAllWithFaqsOrderByIdAsc();
}
