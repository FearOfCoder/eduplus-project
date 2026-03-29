package com.example.doan10diem.repository;

import com.example.doan10diem.model.Role;
import com.example.doan10diem.model.User;
import com.example.doan10diem.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    long countByRoleAndStatus(Role role, UserStatus status);

    Optional<User> findByVerificationCode(String verificationCode);
}

