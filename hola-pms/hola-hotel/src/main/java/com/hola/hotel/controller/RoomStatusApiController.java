package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.service.RoomStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 객실 상태 REST API
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-status")
@RequiredArgsConstructor
public class RoomStatusApiController {

    private final RoomStatusService roomStatusService;
    private final AccessControlService accessControlService;

    /**
     * 객실 HK/FO 상태 변경
     */
    @PutMapping("/{roomNumberId}")
    public HolaResponse<Void> updateStatus(
            @PathVariable Long propertyId,
            @PathVariable Long roomNumberId,
            @RequestBody Map<String, String> request) {
        accessControlService.validatePropertyAccess(propertyId);
        roomStatusService.updateRoomStatus(roomNumberId, propertyId,
                request.get("hkStatus"), request.get("foStatus"), request.get("memo"));
        return HolaResponse.success();
    }

    /**
     * 상태별 객실 수 집계 (VC/VD/OC/OD/OOO/OOS)
     */
    @GetMapping("/summary")
    public HolaResponse<Map<String, Long>> getSummary(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomStatusService.getStatusSummary(propertyId));
    }
}
