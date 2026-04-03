package com.hola.reservation.service;

import com.hola.hotel.dto.response.RoomRackFloorGroupResponse;
import com.hola.hotel.dto.response.RoomRackItemResponse;

import java.util.List;

/**
 * Room Rack 비즈니스 서비스 인터페이스
 * - 객실 현황 조회 (hotel + reservation + HK 데이터 합성)
 */
public interface RoomRackService {

    /**
     * Room Rack 전체 조회 (층별 그룹핑)
     * @param propertyId 프로퍼티 ID
     * @return 층별 객실 현황 리스트
     */
    List<RoomRackFloorGroupResponse> getRoomRack(Long propertyId);

    /**
     * 개별 객실 상세 조회
     * @param propertyId 프로퍼티 ID
     * @param roomNumberId 객실번호 ID
     * @return 객실 상세 현황
     */
    RoomRackItemResponse getRoomRackItem(Long propertyId, Long roomNumberId);
}
