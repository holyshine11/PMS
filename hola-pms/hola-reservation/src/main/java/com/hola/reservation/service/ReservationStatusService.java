package com.hola.reservation.service;

import com.hola.reservation.dto.request.ReservationStatusRequest;
import com.hola.reservation.dto.response.AdminCancelPreviewResponse;

/**
 * 예약 상태 관리 서비스 — 상태 전환, 취소, 노쇼 처리
 */
public interface ReservationStatusService {

    /** 예약 상태 변경 (체크인/투숙/체크아웃/취소/노쇼) */
    void changeStatus(Long id, Long propertyId, ReservationStatusRequest request);

    /** 취소/노쇼 수수료 미리보기 */
    AdminCancelPreviewResponse getCancelPreview(Long id, Long propertyId, boolean noShow, Long subReservationId);

    /** 예약 취소 (soft delete) */
    void cancel(Long id, Long propertyId);
}
