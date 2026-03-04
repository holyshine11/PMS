package com.hola.hotel.service;

import com.hola.hotel.dto.request.RoomNumberRequest;
import com.hola.hotel.dto.response.RoomNumberResponse;

import java.util.List;

public interface RoomNumberService {

    List<RoomNumberResponse> getRoomNumbers(Long propertyId);

    RoomNumberResponse getRoomNumber(Long id);

    RoomNumberResponse createRoomNumber(Long propertyId, RoomNumberRequest request);

    RoomNumberResponse updateRoomNumber(Long id, RoomNumberRequest request);

    void deleteRoomNumber(Long id);

    /** 호수 코드명 중복 확인 */
    boolean existsRoomNumber(Long propertyId, String roomNumber);
}
