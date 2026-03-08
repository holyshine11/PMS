package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.RoomClassCreateRequest;
import com.hola.room.dto.request.RoomClassUpdateRequest;
import com.hola.room.dto.response.RoomClassResponse;
import com.hola.room.service.RoomClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 객실 클래스 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-classes")
@RequiredArgsConstructor
public class RoomClassApiController {

    private final AccessControlService accessControlService;
    private final RoomClassService roomClassService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<RoomClassResponse>>> getRoomClasses(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<RoomClassResponse> list = roomClassService.getRoomClasses(propertyId);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomClassResponse>> getRoomClass(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomClassService.getRoomClass(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<RoomClassResponse>> createRoomClass(
            @PathVariable Long propertyId,
            @Valid @RequestBody RoomClassCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        RoomClassResponse response = roomClassService.createRoomClass(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomClassResponse>> updateRoomClass(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody RoomClassUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomClassService.updateRoomClass(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRoomClass(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        roomClassService.deleteRoomClass(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String roomClassCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = roomClassService.existsRoomClassCode(propertyId, roomClassCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
