package com.example.doan10diem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    @NotBlank
    @Size(min = 5, max = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    @NotBlank
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status = CourseStatus.PENDING_REVIEW;

    /**
     * Lời nói đầu hoặc lý do tạo khóa học gửi Admin duyệt.
     */
    @Column(name = "preface", columnDefinition = "TEXT")
    private String preface;

    /**
     * Giá khóa học (VNĐ).
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal price;

    /**
     * Tỷ lệ % mà platform ăn trên giá khóa học (ví dụ 20 nghĩa là platform ăn 20%).
     */
    @Column(nullable = false)
    @Min(0)
    private Integer platformFeePercent = 20;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    /**
     * URL ảnh đại diện (thumbnail) của khóa học.
     */
    @Column(length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Lesson> lessons = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();
}

