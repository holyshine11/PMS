package com.hola.hotel.service;

import com.hola.hotel.dto.request.FloorRequest;
import com.hola.hotel.dto.response.FloorResponse;

import java.util.List;

public interface FloorService {

    List<FloorResponse> getFloors(Long propertyId);

    FloorResponse getFloor(Long id);

    FloorResponse createFloor(Long propertyId, FloorRequest request);

    FloorResponse updateFloor(Long id, FloorRequest request);

    void deleteFloor(Long id);

    /** 층코드 중복 확인 */
    boolean existsFloorNumber(Long propertyId, String floorNumber);
}
