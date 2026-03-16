package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.PaidServiceOptionCreateRequest;
import com.hola.room.dto.request.PaidServiceOptionUpdateRequest;
import com.hola.room.dto.response.PaidServiceOptionResponse;
import com.hola.room.service.PaidServiceOptionService;
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
 * 유료 서비스 옵션 REST API 컨트롤러
 */
@Tag(name = "유료 서비스 옵션", description = "유료 서비스(미니바, 세탁 등) 옵션 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/paid-service-options")
@RequiredArgsConstructor
public class PaidServiceOptionApiController {

    private final AccessControlService accessControlService;
    private final PaidServiceOptionService paidServiceOptionService;

    @Operation(summary = "유료 옵션 목록 조회", description = "프로퍼티 유료 서비스 옵션 전체 목록 (roomTypeId로 필터링 가능)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<PaidServiceOptionResponse>>> getPaidServiceOptions(
            @PathVariable Long propertyId,
            @RequestParam(required = false) Long roomTypeId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<PaidServiceOptionResponse> list = paidServiceOptionService.getPaidServiceOptions(propertyId, roomTypeId);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @Operation(summary = "유료 옵션 상세 조회", description = "유료 서비스 옵션 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<PaidServiceOptionResponse>> getPaidServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(paidServiceOptionService.getPaidServiceOption(id)));
    }

    @Operation(summary = "유료 옵션 등록", description = "새 유료 서비스 옵션 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<PaidServiceOptionResponse>> createPaidServiceOption(
            @PathVariable Long propertyId,
            @Valid @RequestBody PaidServiceOptionCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        PaidServiceOptionResponse response = paidServiceOptionService.createPaidServiceOption(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "유료 옵션 수정", description = "유료 서비스 옵션 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<PaidServiceOptionResponse>> updatePaidServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody PaidServiceOptionUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(paidServiceOptionService.updatePaidServiceOption(id, request)));
    }

    @Operation(summary = "유료 옵션 삭제", description = "유료 서비스 옵션 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deletePaidServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        paidServiceOptionService.deletePaidServiceOption(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "옵션코드 중복 확인", description = "프로퍼티 내 유료 서비스 옵션코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String serviceOptionCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = paidServiceOptionService.existsServiceOptionCode(propertyId, serviceOptionCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
