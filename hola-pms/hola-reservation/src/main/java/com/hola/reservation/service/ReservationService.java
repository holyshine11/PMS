package com.hola.reservation.service;

import com.hola.reservation.dto.request.*;
import com.hola.reservation.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 예약 관리 서비스 인터페이스
 */
public interface ReservationService {

    /** 예약 리스트 조회 (프로퍼티별) */
    List<ReservationListResponse> getList(Long propertyId, String status, LocalDate checkInFrom,
                                           LocalDate checkInTo, String keyword);

    /** 예약 상세 조회 */
    ReservationDetailResponse getById(Long id, Long propertyId);

    /** 예약 등록 */
    ReservationDetailResponse create(Long propertyId, ReservationCreateRequest request);

    /** 예약 수정 */
    ReservationDetailResponse update(Long id, Long propertyId, ReservationUpdateRequest request);

    /** 예약 취소 (soft delete) */
    void cancel(Long id, Long propertyId);

    /** 예약 상태 변경 */
    void changeStatus(Long id, Long propertyId, ReservationStatusRequest request);

    /** 서브 예약(객실 레그) 추가 */
    SubReservationResponse addLeg(Long reservationId, Long propertyId, SubReservationRequest request);

    /** 서브 예약 수정 */
    SubReservationResponse updateLeg(Long reservationId, Long propertyId, Long legId, SubReservationRequest request);

    /** 서브 예약 삭제 */
    void deleteLeg(Long reservationId, Long propertyId, Long legId);

    /** 객실 가용성 조회 */
    int checkAvailability(Long propertyId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut);

    /** 예약 메모 조회 */
    List<ReservationMemoResponse> getMemos(Long reservationId, Long propertyId);

    /** 예약 메모 등록 */
    ReservationMemoResponse addMemo(Long reservationId, Long propertyId, String content);

    /** 예치금 등록 */
    ReservationDepositResponse addDeposit(Long reservationId, Long propertyId, ReservationDepositRequest request);

    /** 예치금 수정 */
    ReservationDepositResponse updateDeposit(Long reservationId, Long propertyId, Long depositId, ReservationDepositRequest request);

    /** 캘린더뷰: 기간 내 예약을 날짜별로 그룹핑하여 반환 (이름 마스킹) */
    Map<String, List<ReservationCalendarResponse>> getCalendarData(
            Long propertyId, LocalDate startDate, LocalDate endDate,
            String status, String keyword);
}
