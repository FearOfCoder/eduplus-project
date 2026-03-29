package com.example.doan10diem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_assignment_student", columnNames = {"assignment_id", "student_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * Bài làm có thể là text, link tới file, v.v. (mô phỏng).
     */
    @Column(name = "student_answer", nullable = false, length = 4000)
    @NotBlank
    private String studentAnswer;

    /**
     * Cột cũ trong database (để tương thích).
     * Tự động sync với studentAnswer khi save.
     */
    @Column(name = "content", nullable = false, length = 4000, insertable = true, updatable = true)
    private String content;

    /**
     * Tự động đồng bộ content với studentAnswer trước khi persist hoặc update.
     */
    @PrePersist
    @PreUpdate
    private void syncContent() {
        if (this.studentAnswer != null) {
            this.content = this.studentAnswer;
        }
    }

    private LocalDateTime submittedAt;

    /**
     * Điểm do AI chấm (gợi ý, không ghi đè điểm chính thức).
     */
    private Double aiGrade;

    /**
     * Nhận xét của AI (TEXT, chỉ tham khảo).
     */
    @Lob
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    public Double getAiGrade() {
        return aiGrade;
    }

    public void setAiGrade(Double aiGrade) {
        this.aiGrade = aiGrade;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    /**
     * Điểm chính thức (0-100) do giảng viên chốt.
     */
    private Double grade;

    /**
     * Phản hồi của giảng viên (nhận xét chính thức).
     */
    @Column(length = 2000)
    private String teacherFeedback;

    private LocalDateTime gradedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;
}

