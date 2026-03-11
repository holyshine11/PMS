package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.response.RoomNumberResponse;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.room.repository.RoomTypeFloorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 층/호수 배정용 API (예약 상세 페이지에서 사용)
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/floors")
@RequiredArgsConstructor
public class RoomAssignApiController {

    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final HotelMapper hotelMapper;
    private final AccessControlService accessControlService;

    /** 특정 층에 매핑된 호수 목록 조회 */
    @GetMapping("/{floorId}/room-numbers")
    public HolaResponse<List<RoomNumberResponse>> getRoomNumbersByFloor(
            @PathVariable Long propertyId,
            @PathVariable Long floorId) {
        accessControlService.validatePropertyAccess(propertyId);

        // rm_room_type_floor에서 해당 층에 매핑된 호수 ID 조회
        List<Long> roomNumberIds = roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(floorId);
        if (roomNumberIds.isEmpty()) {
            return HolaResponse.success(Collections.emptyList());
        }

        List<RoomNumberResponse> responses = roomNumberRepository.findAllById(roomNumberIds).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());

        return HolaResponse.success(responses);
    }
}
