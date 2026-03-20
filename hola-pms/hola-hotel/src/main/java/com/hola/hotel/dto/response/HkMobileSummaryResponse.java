package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 하우스키퍼 일일 요약 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkMobileSummaryResponse {

    private int totalTasks;
    private int completedTasks;
    private double completionRate;
    private BigDecimal totalCredits;
    private Double avgDurationMinutes;
}
