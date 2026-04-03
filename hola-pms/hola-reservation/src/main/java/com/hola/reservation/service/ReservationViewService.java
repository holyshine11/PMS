package com.hola.reservation.service;

import com.hola.reservation.dto.response.ReservationCalendarResponse;
import com.hola.reservation.dto.response.ReservationTimelineResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 예약 뷰 서비스 — 캘린더/타임라인 뷰 데이터
 */
public interface ReservationViewService {

    /** 캘린더뷰: 기간 내 예약을 날짜별로 그룹핑하여 반환 (이름 마스킹) */
    Map<String, List<ReservationCalendarResponse>> getCalendarData(
            Long propertyId, LocalDate startDate, LocalDate endDate,
            String status, String keyword);

    /** 타임라인뷰: 기간 내 예약을 객실별로 그룹핑하여 반환 (Y축=객실, X축=날짜) */
    ReservationTimelineResponse getTimelineData(
            Long propertyId, LocalDate startDate, LocalDate endDate,
            String status, String keyword);
}
