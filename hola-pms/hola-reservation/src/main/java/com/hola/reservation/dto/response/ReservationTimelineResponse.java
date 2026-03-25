package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 타임라인뷰용 예약 응답 DTO (Y축=객실, X축=날짜)
 * 객실별 예약 목록 + 미배정 예약 분리
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationTimelineResponse {

    /** 객실별 예약 목록 (층 → 호수 순 정렬) */
    private List<TimelineRoom> rooms;

    /** 객실 미배정 예약 목록 */
    private List<ReservationCalendarResponse> unassigned;

    /**
     * 타임라인 객실 행 (1객실 = 1행)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelineRoom {
        private Long roomId;
        private String roomNumber;
        private String floorName;
        private String roomTypeName;
        private List<ReservationCalendarResponse> reservations;
    }
}
