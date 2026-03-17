package com.hola.hotel.service;

import com.hola.hotel.dto.response.RoomRackItemResponse;
import com.hola.hotel.entity.RoomNumber;

import java.util.List;
import java.util.Map;

/**
 * 객실 상태 관리 서비스 인터페이스
 */
public interface RoomStatusService {

    /** HK/FO 상태 변경 */
    void updateRoomStatus(Long roomNumberId, Long propertyId, String hkStatus, String foStatus, String memo);

    /** 상태별 객실 수 집계 (VC, VD, OC, OD, OOO, OOS) */
    Map<String, Long> getStatusSummary(Long propertyId);

    /** Room Rack: 전체 객실 기본 정보 (투숙객 정보 없이) */
    List<RoomRackItemResponse> getRoomRackItems(Long propertyId);

    /** 상태코드 계산 유틸 */
    static String calcStatusCode(String hkStatus, String foStatus) {
        if ("OOO".equals(hkStatus)) return "OOO";
        if ("OOS".equals(hkStatus)) return "OOS";
        String fo = "OCCUPIED".equals(foStatus) ? "O" : "V";
        String hk = "CLEAN".equals(hkStatus) ? "C" : "D";
        return fo + hk;
    }
}
