package com.hola.hotel.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 하우스키핑 일일 출근부 응답 DTO
 */
@Getter
@Builder
public class HkAttendanceResponse {

    private LocalDate date;
    private int totalCount;
    private int availableCount;
    private List<AttendanceEntry> entries;

    @Getter
    @Builder
    public static class AttendanceEntry {
        private Long housekeeperId;
        private String userName;
        private String role;
        private Boolean isAvailable;
        private String shiftType;
        private String note;
        private String sectionName;  // 소속 구역명
        private String attendanceStatus;  // BEFORE_WORK, WORKING, LEFT, DAY_OFF
        private LocalDateTime clockInAt;
        private LocalDateTime clockOutAt;
    }
}
