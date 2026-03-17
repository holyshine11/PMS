package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 대시보드 7일 예약 추이 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardPickupResponse {

    private List<DailyPickup> dailyPickups;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPickup {
        private LocalDate date;
        private long reservationCount; // 해당 날짜 체류 예약 수
        private BigDecimal revenue;    // 해당 날짜 DailyCharge 합계
    }
}
