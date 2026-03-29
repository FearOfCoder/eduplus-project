package com.example.doan10diem.repository;

import com.example.doan10diem.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * Tìm cấu hình theo key.
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
}
