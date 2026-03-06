package com.hola.room.service;

import com.hola.room.dto.request.RoomTypeCreateRequest;
import com.hola.room.dto.request.RoomTypeUpdateRequest;
import com.hola.room.dto.response.RoomTypeListResponse;
import com.hola.room.dto.response.RoomTypeResponse;

import java.util.List;

/**
 * 객실 타입 서비스 인터페이스
 */
public interface RoomTypeService {

    List<RoomTypeListResponse> getRoomTypes(Long propertyId);

    RoomTypeResponse getRoomType(Long id);

    RoomTypeResponse createRoomType(Long propertyId, RoomTypeCreateRequest request);

    RoomTypeResponse updateRoomType(Long id, RoomTypeUpdateRequest request);

    void deleteRoomType(Long id);

    boolean existsRoomTypeCode(Long propertyId, String roomTypeCode);
}
