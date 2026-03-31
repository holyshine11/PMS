package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.dto.PageInfo;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.RoomNumberRequest;
import com.hola.hotel.dto.response.RoomNumberResponse;
import com.hola.hotel.service.RoomNumberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "호수 관리", description = "프로퍼티 호수(객실번호) 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-numbers")
@RequiredArgsConstructor
public class RoomNumberApiController {

    private final RoomNumberService roomNumberService;
    private final AccessControlService accessControlService;

    @Operation(summary = "호수 목록 조회", description = "프로퍼티 호수 전체 목록 (페이징)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<RoomNumberResponse>>> getRoomNumbers(
            @PathVariable Long propertyId,
            @PageableDefault(size = 20) Pageable pageable) {
        accessControlService.validatePropertyAccess(propertyId);
        Page<RoomNumberResponse> page = roomNumberService.getRoomNumbers(propertyId, pageable);
        return ResponseEntity.ok(HolaResponse.success(page.getContent(), PageInfo.from(page)));
    }

    @Operation(summary = "호수 상세 조회", description = "호수 ID로 상세 정보 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomNumberResponse>> getRoomNumber(@PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomNumberService.getRoomNumber(id)));
    }

    @Operation(summary = "호수 등록", description = "새 호수(객실번호) 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<RoomNumberResponse>> createRoomNumber(
            @PathVariable Long propertyId, @Valid @RequestBody RoomNumberRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(roomNumberService.createRoomNumber(propertyId, request)));
    }

    @Operation(summary = "호수 수정", description = "호수 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomNumberResponse>> updateRoomNumber(
            @PathVariable Long propertyId, @PathVariable Long id, @Valid @RequestBody RoomNumberRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(roomNumberService.updateRoomNumber(id, request)));
    }

    @Operation(summary = "호수 삭제", description = "호수 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRoomNumber(@PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        roomNumberService.deleteRoomNumber(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 호수코드 중복 확인 */
    @Operation(summary = "호수코드 중복 확인", description = "프로퍼티 내 호수코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkRoomNumberCode(
            @PathVariable Long propertyId,
            @RequestParam String roomNumber) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = roomNumberService.existsRoomNumber(propertyId, roomNumber);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
