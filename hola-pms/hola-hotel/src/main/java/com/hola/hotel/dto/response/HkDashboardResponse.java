package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 하우스키핑 대시보드 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkDashboardResponse {

    private int totalTasks;
    private int pendingTasks;
    private int inProgressTasks;
    private int completedTasks;
    private int inspectedTasks;
    private int cancelledTasks;
    private double completionRate;  // 완료율 (%)

    private List<HousekeeperSummary> housekeeperSummaries;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HousekeeperSummary {
        private Long userId;
        private String userName;
        private int pendingCount;
        private int inProgressCount;
        private int completedCount;
        private BigDecimal totalCredits;
        private Double avgDurationMinutes;
        private String attendanceStatus;   // BEFORE_WORK, WORKING, LEFT, DAY_OFF
        private LocalDateTime clockInAt;
        private LocalDateTime clockOutAt;
    }
}
