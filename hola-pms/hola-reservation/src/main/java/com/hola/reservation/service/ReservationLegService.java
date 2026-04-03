package com.hola.reservation.service;

import com.hola.reservation.dto.request.SubReservationRequest;
import com.hola.reservation.dto.response.SubReservationResponse;

/**
 * 서브 예약(Leg) 관리 서비스 — 추가/수정/삭제
 */
public interface ReservationLegService {

    /** 서브 예약(객실 레그) 추가 */
    SubReservationResponse addLeg(Long reservationId, Long propertyId, SubReservationRequest request);

    /** 서브 예약 수정 */
    SubReservationResponse updateLeg(Long reservationId, Long propertyId, Long legId, SubReservationRequest request);

    /** 서브 예약 삭제 */
    void deleteLeg(Long reservationId, Long propertyId, Long legId);

    /** 얼리/레이트 요금 등록 (시간대 선택 즉시 확정) */
    java.math.BigDecimal registerEarlyLateFee(Long reservationId, Long propertyId, Long legId,
                                               String policyType, int policyIndex);

    /** 얼리/레이트 요금 해제 */
    void removeEarlyLateFee(Long reservationId, Long propertyId, Long legId, String policyType);
}
