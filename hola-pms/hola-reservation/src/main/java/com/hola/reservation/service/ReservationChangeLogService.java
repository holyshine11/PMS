package com.hola.reservation.service;

import com.hola.reservation.dto.response.ReservationChangeLogResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ReservationChangeLogService {

    /**
     * 범용 변경 이력 기록
     */
    void log(Long masterReservationId, Long subReservationId,
             String category, String changeType, String fieldName,
             String oldValue, String newValue, String description);

    /**
     * 필드 변경 비교 후 기록 (값이 같으면 skip)
     */
    void logFieldChange(Long masterReservationId, Long subReservationId,
                        String category, String fieldName,
                        Object oldValue, Object newValue, String fieldLabel);

    /**
     * 상태 변경 기록
     */
    void logStatusChange(Long masterReservationId, Long subReservationId,
                         String oldStatus, String newStatus);

    /**
     * 업그레이드 기록
     */
    void logUpgrade(Long masterReservationId, Long subReservationId,
                    String fromRoomType, String toRoomType,
                    String upgradeType, BigDecimal priceDiff);

    /**
     * 변경 이력 조회
     */
    List<ReservationChangeLogResponse> getHistory(Long masterReservationId);
}
