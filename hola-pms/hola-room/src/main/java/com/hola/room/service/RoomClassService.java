package com.hola.room.service;

import com.hola.room.dto.request.RoomClassCreateRequest;
import com.hola.room.dto.request.RoomClassUpdateRequest;
import com.hola.room.dto.response.RoomClassResponse;

import java.util.List;

/**
 * 객실 클래스 서비스 인터페이스
 */
public interface RoomClassService {

    List<RoomClassResponse> getRoomClasses(Long propertyId);

    RoomClassResponse getRoomClass(Long id);

    RoomClassResponse createRoomClass(Long propertyId, RoomClassCreateRequest request);

    RoomClassResponse updateRoomClass(Long id, RoomClassUpdateRequest request);

    void deleteRoomClass(Long id);

    boolean existsRoomClassCode(Long propertyId, String roomClassCode);
}
