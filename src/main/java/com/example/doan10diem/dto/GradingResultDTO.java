package com.example.doan10diem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa kết quả chấm điểm từ AI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingResultDTO {
    /**
     * Điểm số (0-10 hoặc 0-100, tùy theo scale).
     */
    private Double score;

    /**
     * Nhận xét chi tiết từ AI.
     */
    private String feedback;
}
