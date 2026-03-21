package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.RoomUnavailableRequest;
import com.hola.hotel.dto.response.RoomUnavailableResponse;
import com.hola.hotel.service.RoomUnavailableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OOO/OOS 객실 관리 REST API
 */
@Tag(name = "객실 사용불가", description = "OOO/OOS 객실 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-unavailable")
@RequiredArgsConstructor
public class RoomUnavailableApiController {

    private final RoomUnavailableService roomUnavailableService;
    private final AccessControlService accessControlService;

    @Operation(summary = "OOO/OOS 목록 조회", description = "사용불가 객실 목록")
    @GetMapping
    public HolaResponse<List<RoomUnavailableResponse>> getList(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String type) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.getList(propertyId, type));
    }

    @Operation(summary = "OOO/OOS 상세 조회", description = "사용불가 상세 정보")
    @GetMapping("/{id}")
    public HolaResponse<RoomUnavailableResponse> getById(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.getById(id, propertyId));
    }

    @Operation(summary = "OOO/OOS 등록", description = "객실 사용불가 기간 등록")
    @PostMapping
    public HolaResponse<RoomUnavailableResponse> create(
            @PathVariable Long propertyId, @RequestBody RoomUnavailableRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.create(propertyId, request));
    }

    @Operation(summary = "OOO/OOS 수정", description = "사용불가 기간/사유 수정")
    @PutMapping("/{id}")
    public HolaResponse<RoomUnavailableResponse> update(
            @PathVariable Long propertyId, @PathVariable Long id,
            @RequestBody RoomUnavailableRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.update(id, propertyId, request));
    }

    @Operation(summary = "OOO/OOS 삭제", description = "사용불가 해제")
    @DeleteMapping("/{id}")
    public HolaResponse<Void> delete(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        roomUnavailableService.delete(id, propertyId);
        return HolaResponse.success();
    }
}
