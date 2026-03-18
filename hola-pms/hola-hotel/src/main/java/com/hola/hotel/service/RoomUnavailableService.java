package com.hola.hotel.service;

import com.hola.hotel.dto.request.RoomUnavailableRequest;
import com.hola.hotel.dto.response.RoomUnavailableResponse;

import java.util.List;

/**
 * OOO/OOS 객실 관리 서비스
 */
public interface RoomUnavailableService {

    List<RoomUnavailableResponse> getList(Long propertyId, String type);

    RoomUnavailableResponse getById(Long id, Long propertyId);

    RoomUnavailableResponse create(Long propertyId, RoomUnavailableRequest request);

    RoomUnavailableResponse update(Long id, Long propertyId, RoomUnavailableRequest request);

    void delete(Long id, Long propertyId);

    /**
     * 만료된 OOO/OOS 자동 해제 (return_status로 HK 상태 복귀)
     */
    int releaseExpired(Long propertyId);
}
