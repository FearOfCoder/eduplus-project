package com.example.doan10diem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 150)
    @NotBlank
    @Size(min = 3, max = 150)
    private String title;

    /**
     * Nội dung ngắn, mô tả, text,...
     */
    @Column(length = 4000)
    private String content;

    /**
     * URL video hoặc đường dẫn tài liệu (mô phỏng).
     */
    @Column(length = 1000)
    private String resourceUrl;

    @Enumerated(EnumType.STRING)
    @NotNull
    private LessonType type;

    @Column(name = "sequence_order")
    private Integer sortOrder;
}

