package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 캘린더 조회 응답 (산하 2.2 + 2.3 통합)
 * - 판매 가능 날짜 목록
 * - 체크인/체크아웃 분리 모드 지원
 */
@Getter
@Builder
@AllArgsConstructor
public class CalendarResponse {

    /** 프로퍼티 코드 */
    private final String propertyCode;

    /** 조회 시작일 */
    private final LocalDate startDate;

    /** 조회 종료일 */
    private final LocalDate endDate;

    /** 날짜별 가용 정보 */
    private final List<DateAvailability> dates;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DateAvailability {
        /** 날짜 */
        private final LocalDate date;

        /** 체크인 가능 여부 */
        private final boolean checkInAvailable;

        /** 체크아웃 가능 여부 */
        private final boolean checkOutAvailable;

        /** 가용 객실타입 수 */
        private final int availableRoomTypes;

        /** 최저가 (KRW 기준) */
        private final Long minPrice;
    }
}
