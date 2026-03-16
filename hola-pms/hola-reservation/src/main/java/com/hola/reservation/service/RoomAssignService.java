package com.hola.reservation.service;

import com.hola.reservation.booking.dto.response.RoomAssignAvailabilityResponse;
import com.hola.reservation.dto.response.RoomNumberAvailabilityResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 객실 배정 서비스 인터페이스
 */
public interface RoomAssignService {

    /**
     * 객실 배정 가용성 조회
     *
     * @param propertyId   프로퍼티 ID
     * @param roomTypeId   현재 레그의 객실타입 ID
     * @param rateCodeId   레이트코드 ID
     * @param checkIn      체크인일
     * @param checkOut     체크아웃일
     * @param adults       성인 수
     * @param children     아동 수
     * @param excludeSubId 제외할 서브예약 ID (자기 자신 제외)
     * @return 객실 배정 가용성 응답
     */
    RoomAssignAvailabilityResponse getAvailability(
            Long propertyId, Long roomTypeId, Long rateCodeId,
            LocalDate checkIn, LocalDate checkOut,
            int adults, int children, Long excludeSubId);

    /**
     * 층별 호수 가용성 조회
     */
    List<RoomNumberAvailabilityResponse> getFloorRoomAvailability(
            Long floorId, LocalDate checkIn, LocalDate checkOut, Long excludeSubId);
}
