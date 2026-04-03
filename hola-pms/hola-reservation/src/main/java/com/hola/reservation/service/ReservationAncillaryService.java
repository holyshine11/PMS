package com.hola.reservation.service;

import com.hola.reservation.dto.request.ReservationDepositRequest;
import com.hola.reservation.dto.request.ReservationServiceRequest;
import com.hola.reservation.dto.response.ReservationDepositResponse;
import com.hola.reservation.dto.response.ReservationMemoResponse;
import com.hola.reservation.dto.response.ReservationServiceResponse;

import java.util.List;

/**
 * 예약 부속 서비스 — 메모, 예치금, 유료 서비스 관리
 */
public interface ReservationAncillaryService {

    /** 예약 메모 조회 */
    List<ReservationMemoResponse> getMemos(Long reservationId, Long propertyId);

    /** 예약 메모 등록 */
    ReservationMemoResponse addMemo(Long reservationId, Long propertyId, String content);

    /** 예치금 등록 */
    ReservationDepositResponse addDeposit(Long reservationId, Long propertyId, ReservationDepositRequest request);

    /** 예치금 수정 */
    ReservationDepositResponse updateDeposit(Long reservationId, Long propertyId, Long depositId, ReservationDepositRequest request);

    /** 유료 서비스 추가 */
    ReservationServiceResponse addService(Long masterReservationId, Long subReservationId, Long propertyId, ReservationServiceRequest request);

    /** 유료 서비스 삭제 */
    void removeService(Long masterReservationId, Long subReservationId, Long serviceId, Long propertyId);
}
