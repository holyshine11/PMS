package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 하우스키핑 일일 출근부 저장 요청 DTO
 */
@Getter
@Setter
public class HkAttendanceRequest {

    @NotNull(message = "날짜는 필수입니다.")
    private LocalDate date;

    private List<AttendanceEntry> entries;

    @Getter
    @Setter
    public static class AttendanceEntry {
        private Long housekeeperId;
        private Boolean isAvailable;
        private String shiftType;  // DAY, EVENING, NIGHT
        private String note;
    }
}
