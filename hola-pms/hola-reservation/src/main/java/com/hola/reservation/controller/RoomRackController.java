package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.PropertyAccess;
import com.hola.hotel.dto.response.RoomRackFloorGroupResponse;
import com.hola.hotel.dto.response.RoomRackItemResponse;
import com.hola.reservation.service.RoomRackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Room Rack REST API (hotel + reservation 데이터 합성)
 */
@Tag(name = "룸랙", description = "실시간 객실 현황 조회 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-rack")
@RequiredArgsConstructor
public class RoomRackController {

    private final RoomRackService roomRackService;

    /**
     * Room Rack 전체 조회 (층별 그룹핑)
     */
    @Operation(summary = "룸랙 조회", description = "전체 객실 현황 (점유/청소/OOO/OOS 상태)")
    @GetMapping
    @PropertyAccess
    public HolaResponse<List<RoomRackFloorGroupResponse>> getRoomRack(@PathVariable Long propertyId) {
        List<RoomRackFloorGroupResponse> result = roomRackService.getRoomRack(propertyId);
        return HolaResponse.success(result);
    }

    /**
     * 개별 객실 상세
     */
    @Operation(summary = "객실 상세 조회", description = "개별 객실 상세 현황")
    @GetMapping("/{roomNumberId}")
    @PropertyAccess
    public HolaResponse<RoomRackItemResponse> getRoomRackItem(
            @PathVariable Long propertyId, @PathVariable Long roomNumberId) {
        RoomRackItemResponse item = roomRackService.getRoomRackItem(propertyId, roomNumberId);
        return HolaResponse.success(item);
    }
}
