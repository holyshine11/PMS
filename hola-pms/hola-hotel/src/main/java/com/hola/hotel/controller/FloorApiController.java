package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.FloorRequest;
import com.hola.hotel.dto.response.FloorResponse;
import com.hola.hotel.service.FloorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "층 관리", description = "프로퍼티 층코드 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/floors")
@RequiredArgsConstructor
public class FloorApiController {

    private final FloorService floorService;
    private final AccessControlService accessControlService;

    @Operation(summary = "층 목록 조회", description = "프로퍼티 층코드 전체 목록")
    @GetMapping
    public ResponseEntity<HolaResponse<List<FloorResponse>>> getFloors(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(floorService.getFloors(propertyId)));
    }

    @Operation(summary = "층 상세 조회", description = "층코드 ID로 상세 정보 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<FloorResponse>> getFloor(@PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(floorService.getFloor(id)));
    }

    @Operation(summary = "층 등록", description = "새 층코드 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<FloorResponse>> createFloor(
            @PathVariable Long propertyId, @Valid @RequestBody FloorRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(floorService.createFloor(propertyId, request)));
    }

    @Operation(summary = "층 수정", description = "층코드 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<FloorResponse>> updateFloor(
            @PathVariable Long propertyId, @PathVariable Long id, @Valid @RequestBody FloorRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(floorService.updateFloor(id, request)));
    }

    @Operation(summary = "층 삭제", description = "층코드 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteFloor(@PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        floorService.deleteFloor(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 층코드 중복 확인 */
    @Operation(summary = "층코드 중복 확인", description = "프로퍼티 내 층코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkFloorCode(
            @PathVariable Long propertyId,
            @RequestParam String floorNumber) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = floorService.existsFloorNumber(propertyId, floorNumber);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
