package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.service.RoomStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 객실 상태 REST API
 */
@Tag(name = "객실 상태", description = "객실 HK/FO 상태 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-status")
@RequiredArgsConstructor
public class RoomStatusApiController {

    private final RoomStatusService roomStatusService;
    private final AccessControlService accessControlService;

    /**
     * 객실 HK/FO 상태 변경
     */
    @Operation(summary = "객실 상태 변경", description = "개별 객실 HK/FO 상태 수동 변경. VD 객실은 assigneeId로 담당자 배정 가능")
    @PutMapping("/{roomNumberId}")
    public HolaResponse<Void> updateStatus(
            @PathVariable Long propertyId,
            @PathVariable Long roomNumberId,
            @RequestBody Map<String, String> request) {
        accessControlService.validatePropertyAccess(propertyId);

        // assigneeId 파싱 (선택 파라미터)
        Long assigneeId = null;
        String assigneeIdStr = request.get("assigneeId");
        if (assigneeIdStr != null && !assigneeIdStr.isEmpty()) {
            assigneeId = Long.parseLong(assigneeIdStr);
        }

        roomStatusService.updateRoomStatus(roomNumberId, propertyId,
                request.get("hkStatus"), request.get("foStatus"), request.get("memo"), assigneeId);
        return HolaResponse.success();
    }

    /**
     * 상태별 객실 수 집계 (VC/VD/OC/OD/OOO/OOS)
     */
    @Operation(summary = "객실 상태 요약", description = "HK/FO 상태별 객실 수 요약")
    @GetMapping("/summary")
    public HolaResponse<Map<String, Long>> getSummary(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomStatusService.getStatusSummary(propertyId));
    }
}
