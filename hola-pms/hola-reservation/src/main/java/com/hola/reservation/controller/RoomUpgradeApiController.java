package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.RoomUpgradeRequest;
import com.hola.reservation.dto.response.RoomUpgradeHistoryResponse;
import com.hola.reservation.dto.response.UpgradeAvailableTypeResponse;
import com.hola.reservation.dto.response.UpgradePreviewResponse;
import com.hola.reservation.service.RoomUpgradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "객실 업그레이드", description = "객실 업그레이드 미리보기/실행/이력 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/reservations/{subReservationId}/upgrade")
@RequiredArgsConstructor
public class RoomUpgradeApiController {

    private final AccessControlService accessControlService;
    private final RoomUpgradeService roomUpgradeService;

    @Operation(summary = "업그레이드 가능 객실타입 조회")
    @GetMapping("/available-types")
    public ResponseEntity<HolaResponse<List<UpgradeAvailableTypeResponse>>> getAvailableTypes(
            @PathVariable Long propertyId,
            @PathVariable Long subReservationId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                roomUpgradeService.getAvailableTypes(subReservationId)));
    }

    @Operation(summary = "업그레이드 미리보기", description = "차액 계산 결과를 반환. selectedRateCodeId로 레이트코드 직접 선택 가능")
    @GetMapping("/preview")
    public ResponseEntity<HolaResponse<UpgradePreviewResponse>> previewUpgrade(
            @PathVariable Long propertyId,
            @PathVariable Long subReservationId,
            @RequestParam Long toRoomTypeId,
            @RequestParam(required = false) Long selectedRateCodeId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                roomUpgradeService.previewUpgrade(subReservationId, toRoomTypeId, selectedRateCodeId)));
    }

    @Operation(summary = "업그레이드 실행")
    @PostMapping
    public ResponseEntity<HolaResponse<RoomUpgradeHistoryResponse>> executeUpgrade(
            @PathVariable Long propertyId,
            @PathVariable Long subReservationId,
            @Valid @RequestBody RoomUpgradeRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                roomUpgradeService.executeUpgrade(subReservationId, request)));
    }

    @Operation(summary = "업그레이드 이력 조회")
    @GetMapping("/history")
    public ResponseEntity<HolaResponse<List<RoomUpgradeHistoryResponse>>> getUpgradeHistory(
            @PathVariable Long propertyId,
            @PathVariable Long subReservationId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                roomUpgradeService.getUpgradeHistory(subReservationId)));
    }
}
