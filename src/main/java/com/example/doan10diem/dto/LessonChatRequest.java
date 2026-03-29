package com.example.doan10diem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO request cho API hỏi đáp với AI theo ngữ cảnh bài học.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonChatRequest {

    @NotNull(message = "lessonId không được để trống")
    private Long lessonId;

    @NotBlank(message = "Câu hỏi không được để trống")
    private String question;
}
