package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.ReservationChannelRequest;
import com.hola.hotel.dto.response.ReservationChannelResponse;
import com.hola.hotel.service.ReservationChannelService;
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

@Tag(name = "예약 채널", description = "예약 채널(OTA, 전화, 워크인 등) 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/reservation-channels")
@RequiredArgsConstructor
public class ReservationChannelApiController {

    private final ReservationChannelService reservationChannelService;
    private final AccessControlService accessControlService;

    @Operation(summary = "채널 목록 조회", description = "프로퍼티 예약 채널 전체 목록")
    @GetMapping
    public ResponseEntity<HolaResponse<List<ReservationChannelResponse>>> getList(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(reservationChannelService.getList(propertyId)));
    }

    @Operation(summary = "채널 상세 조회", description = "예약 채널 ID로 상세 정보 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<ReservationChannelResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(reservationChannelService.getById(id)));
    }

    @Operation(summary = "채널 등록", description = "새 예약 채널 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<ReservationChannelResponse>> create(
            @PathVariable Long propertyId, @Valid @RequestBody ReservationChannelRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(reservationChannelService.create(propertyId, request)));
    }

    @Operation(summary = "채널 수정", description = "예약 채널 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<ReservationChannelResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ReservationChannelRequest request) {
        return ResponseEntity.ok(HolaResponse.success(reservationChannelService.update(id, request)));
    }

    @Operation(summary = "채널 삭제", description = "예약 채널 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(@PathVariable Long id) {
        reservationChannelService.delete(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 채널코드 중복 확인 */
    @Operation(summary = "채널코드 중복 확인", description = "프로퍼티 내 채널코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String channelCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = reservationChannelService.checkCode(propertyId, channelCode);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
