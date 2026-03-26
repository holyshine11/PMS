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
    private int unassignedTasks;    // 미배정 작업 수
    private double completionRate;  // 완료율 (%)

    private List<HousekeeperSummary> housekeeperSummaries;

    // 객실 상태 요약 (VD, OD 등 청소 필요 객실 현황)
    private RoomStatusSummary roomStatusSummary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomStatusSummary {
        private int totalRooms;
        private int vacantClean;    // VC: 빈방+청소완료 (판매 가능)
        private int vacantDirty;    // VD: 빈방+청소필요
        private int occupiedClean;  // OC: 투숙중+청소완료
        private int occupiedDirty;  // OD: 투숙중+청소필요
        private int ooo;            // Out of Order
        private int oos;            // Out of Service
        private int dnd;
    }

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
