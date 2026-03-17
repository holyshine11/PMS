package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대시보드 운영현황 응답 DTO
 * 도착/투숙/출발 카운트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOperationResponse {

    private long arrivals;      // 오늘 도착 예정 (RESERVED, checkIn=today)
    private long inHouse;       // 현재 투숙중 (CHECK_IN/INHOUSE)
    private long departures;    // 오늘 출발 예정 (checkOut=today, CHECK_IN/INHOUSE)
    private long checkedInToday;  // 오늘 실제 체크인 완료
    private long checkedOutToday; // 오늘 실제 체크아웃 완료
}
