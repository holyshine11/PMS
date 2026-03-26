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

    /** HK/FO 상태 변경 + 하우스키퍼 담당자 배정 (VD 객실) */
    void updateRoomStatus(Long roomNumberId, Long propertyId, String hkStatus, String foStatus, String memo, Long assigneeId);

    /** 상태별 객실 수 집계 (VC, VD, OC, OD, OOO, OOS) */
    Map<String, Long> getStatusSummary(Long propertyId);

    /** Room Rack: 전체 객실 기본 정보 (투숙객 정보 없이) */
    List<RoomRackItemResponse> getRoomRackItems(Long propertyId);

    /** 상태코드 계산 유틸 (INSPECTED→I, PICKUP→P 확장) */
    static String calcStatusCode(String hkStatus, String foStatus) {
        if ("OOO".equals(hkStatus)) return "OOO";
        if ("OOS".equals(hkStatus)) return "OOS";
        if ("DND".equals(hkStatus)) return "DND";
        String fo = "OCCUPIED".equals(foStatus) ? "O" : "V";
        String hk;
        switch (hkStatus) {
            case "CLEAN": hk = "C"; break;
            case "INSPECTED": hk = "I"; break;
            case "PICKUP": hk = "P"; break;
            default: hk = "D"; break; // DIRTY
        }
        return fo + hk;
    }
}
