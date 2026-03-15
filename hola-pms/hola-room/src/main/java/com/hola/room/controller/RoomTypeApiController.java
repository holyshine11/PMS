package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.RoomTypeCreateRequest;
import com.hola.room.dto.request.RoomTypeUpdateRequest;
import com.hola.room.dto.response.RoomTypeListResponse;
import com.hola.room.dto.response.RoomTypeResponse;
import com.hola.room.service.RoomTypeService;
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
 * 객실 타입 REST API 컨트롤러
 */
@Tag(name = "객실 타입", description = "객실 타입 관리 API (클래스 하위 세부 유형)")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-types")
@RequiredArgsConstructor
public class RoomTypeApiController {

    private final AccessControlService accessControlService;
    private final RoomTypeService roomTypeService;

    @Operation(summary = "타입 목록 조회", description = "프로퍼티 객실 타입 전체 목록")
    @GetMapping
    public ResponseEntity<HolaResponse<List<RoomTypeListResponse>>> getRoomTypes(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomTypeService.getRoomTypes(propertyId)));
    }

    @Operation(summary = "타입 상세 조회", description = "객실 타입 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomTypeResponse>> getRoomType(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomTypeService.getRoomType(id)));
    }

    @Operation(summary = "타입 등록", description = "새 객실 타입 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<RoomTypeResponse>> createRoomType(
            @PathVariable Long propertyId,
            @Valid @RequestBody RoomTypeCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        RoomTypeResponse response = roomTypeService.createRoomType(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "타입 수정", description = "객실 타입 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomTypeResponse>> updateRoomType(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomTypeService.updateRoomType(id, request)));
    }

    @Operation(summary = "타입 삭제", description = "객실 타입 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRoomType(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "타입코드 중복 확인", description = "프로퍼티 내 객실 타입코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String roomTypeCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = roomTypeService.existsRoomTypeCode(propertyId, roomTypeCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
