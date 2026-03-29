package com.example.doan10diem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRevenueDTO {

    private String courseName;
    private Long studentCount;
    private BigDecimal grossTotal;
    private BigDecimal netTotal;

    public BigDecimal getPlatformFee() {
        if (grossTotal == null) return BigDecimal.ZERO;
        if (netTotal == null) return grossTotal;
        return grossTotal.subtract(netTotal);
    }
}
