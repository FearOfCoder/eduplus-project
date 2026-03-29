package com.example.doan10diem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(nullable = false, length = 20)
    private String status = "SUCCESS";

    @Column(length = 500)
    private String reason;
}
