package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 하우스키핑 월별 근태 응답 DTO (행=하우스키퍼, 열=일자)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkMonthlyAttendanceResponse {

    private int year;
    private int month;
    private int daysInMonth;
    private List<HousekeeperRow> rows;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HousekeeperRow {
        private Long housekeeperId;
        private String userName;
        private List<DayCell> days;  // 1~말일 (index 0 = 1일)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayCell {
        private Long attendanceId;     // null이면 미등록
        private int day;
        private String attendanceStatus;  // BEFORE_WORK, WORKING, LEFT, DAY_OFF
        private Boolean isAvailable;
        private String shiftType;
        private LocalDateTime clockInAt;
        private LocalDateTime clockOutAt;
    }
}
