package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.RoomUnavailableRequest;
import com.hola.hotel.dto.response.RoomUnavailableResponse;
import com.hola.hotel.service.RoomUnavailableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OOO/OOS 객실 관리 REST API
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-unavailable")
@RequiredArgsConstructor
public class RoomUnavailableApiController {

    private final RoomUnavailableService roomUnavailableService;
    private final AccessControlService accessControlService;

    @GetMapping
    public HolaResponse<List<RoomUnavailableResponse>> getList(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String type) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.getList(propertyId, type));
    }

    @GetMapping("/{id}")
    public HolaResponse<RoomUnavailableResponse> getById(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.getById(id, propertyId));
    }

    @PostMapping
    public HolaResponse<RoomUnavailableResponse> create(
            @PathVariable Long propertyId, @RequestBody RoomUnavailableRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.create(propertyId, request));
    }

    @PutMapping("/{id}")
    public HolaResponse<RoomUnavailableResponse> update(
            @PathVariable Long propertyId, @PathVariable Long id,
            @RequestBody RoomUnavailableRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(roomUnavailableService.update(id, propertyId, request));
    }

    @DeleteMapping("/{id}")
    public HolaResponse<Void> delete(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        roomUnavailableService.delete(id, propertyId);
        return HolaResponse.success();
    }
}
