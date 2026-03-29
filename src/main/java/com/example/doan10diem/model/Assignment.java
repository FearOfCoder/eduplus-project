package com.example.doan10diem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull
    private Course course;

    /**
     * Có thể gắn assignment với 1 lesson cụ thể (optional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(nullable = false, length = 150)
    @NotBlank
    private String title;

    @Column(length = 4000)
    private String description;

    /**
     * Đáp án gợi ý hoặc tiêu chí chấm điểm cho bài tập.
     * Giảng viên có thể nhập đáp án mẫu hoặc các tiêu chí để AI chấm điểm tự động.
     */
    @Lob
    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dueDate;

    /**
     * Danh sách bài nộp của sinh viên cho bài tập này.
     * mappedBy = "assignment" tham chiếu tới field assignment trong Submission.
     */
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Submission> submissions = new ArrayList<>();
}

