package com.example.doan10diem.repository;

import com.example.doan10diem.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderByCategory_IdAscIdAsc();
}
