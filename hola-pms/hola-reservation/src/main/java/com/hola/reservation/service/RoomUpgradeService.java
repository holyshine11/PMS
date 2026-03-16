package com.hola.reservation.service;

import com.hola.reservation.dto.request.RoomUpgradeRequest;
import com.hola.reservation.dto.response.RoomUpgradeHistoryResponse;
import com.hola.reservation.dto.response.UpgradeAvailableTypeResponse;
import com.hola.reservation.dto.response.UpgradePreviewResponse;

import java.util.List;

public interface RoomUpgradeService {

    /** 업그레이드 가능 객실타입 목록 */
    List<UpgradeAvailableTypeResponse> getAvailableTypes(Long subReservationId);

    /** 업그레이드 미리보기 (차액 계산) */
    UpgradePreviewResponse previewUpgrade(Long subReservationId, Long toRoomTypeId);

    /** 업그레이드 실행 */
    RoomUpgradeHistoryResponse executeUpgrade(Long subReservationId, RoomUpgradeRequest request);

    /** 업그레이드 이력 조회 */
    List<RoomUpgradeHistoryResponse> getUpgradeHistory(Long subReservationId);
}
