package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.RoomTypeCreateRequest;
import com.hola.room.dto.request.RoomTypeUpdateRequest;
import com.hola.room.dto.response.RoomTypeListResponse;
import com.hola.room.dto.response.RoomTypeResponse;
import com.hola.room.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 객실 타입 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-types")
@RequiredArgsConstructor
public class RoomTypeApiController {

    private final AccessControlService accessControlService;
    private final RoomTypeService roomTypeService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<RoomTypeListResponse>>> getRoomTypes(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomTypeService.getRoomTypes(propertyId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomTypeResponse>> getRoomType(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomTypeService.getRoomType(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<RoomTypeResponse>> createRoomType(
            @PathVariable Long propertyId,
            @Valid @RequestBody RoomTypeCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        RoomTypeResponse response = roomTypeService.createRoomType(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomTypeResponse>> updateRoomType(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomTypeService.updateRoomType(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRoomType(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String roomTypeCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = roomTypeService.existsRoomTypeCode(propertyId, roomTypeCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
