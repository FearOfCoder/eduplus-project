package com.example.doan10diem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal amount;

    /**
     * Mã ngân hàng (VCB, MB, VISA, etc.)
     */
    @Column(length = 50)
    private String bankCode;

    /**
     * Mã giao dịch duy nhất (VD: TXN_20260213...)
     */
    @Column(length = 100, unique = true)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    /**
     * Ngày thanh toán
     */
    private LocalDateTime paymentDate;

    /**
     * Mã giao dịch mô phỏng (deprecated - sử dụng transactionId thay thế).
     */
    @Column(length = 100)
    private String transactionCode;

    /**
     * 4 số cuối thẻ credit card (mô phỏng, KHÔNG lưu full số thẻ trong thực tế).
     */
    @Column(length = 4)
    private String cardLast4Digits;

    /**
     * Chuỗi mô tả ngắn cho lịch sử giao dịch.
     */
    @Column(length = 255)
    @NotBlank
    private String description;

    /**
     * Số tiền platform thu được từ giao dịch này.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal platformShare;

    /**
     * Số tiền giảng viên thu được từ giao dịch này.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal teacherShare;
}

