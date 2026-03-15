package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.RoomClassCreateRequest;
import com.hola.room.dto.request.RoomClassUpdateRequest;
import com.hola.room.dto.response.RoomClassResponse;
import com.hola.room.service.RoomClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "객실 클래스", description = "객실 클래스 관리 API (스탠다드, 디럭스, 스위트 등)")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-classes")
@RequiredArgsConstructor
public class RoomClassApiController {

    private final AccessControlService accessControlService;
    private final RoomClassService roomClassService;

    @Operation(summary = "클래스 목록 조회", description = "프로퍼티 객실 클래스 전체 목록")
    @GetMapping
    public ResponseEntity<HolaResponse<List<RoomClassResponse>>> getRoomClasses(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<RoomClassResponse> list = roomClassService.getRoomClasses(propertyId);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @Operation(summary = "클래스 상세 조회", description = "객실 클래스 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomClassResponse>> getRoomClass(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomClassService.getRoomClass(id)));
    }

    @Operation(summary = "클래스 등록", description = "새 객실 클래스 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<RoomClassResponse>> createRoomClass(
            @PathVariable Long propertyId,
            @Valid @RequestBody RoomClassCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        RoomClassResponse response = roomClassService.createRoomClass(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "클래스 수정", description = "객실 클래스 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomClassResponse>> updateRoomClass(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody RoomClassUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomClassService.updateRoomClass(id, request)));
    }

    @Operation(summary = "클래스 삭제", description = "객실 클래스 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRoomClass(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        roomClassService.deleteRoomClass(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "클래스코드 중복 확인", description = "프로퍼티 내 객실 클래스코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String roomClassCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = roomClassService.existsRoomClassCode(propertyId, roomClassCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
