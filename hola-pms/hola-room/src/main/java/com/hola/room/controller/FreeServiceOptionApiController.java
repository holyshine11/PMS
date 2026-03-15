package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.FreeServiceOptionCreateRequest;
import com.hola.room.dto.request.FreeServiceOptionUpdateRequest;
import com.hola.room.dto.response.FreeServiceOptionResponse;
import com.hola.room.service.FreeServiceOptionService;
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
 * 무료 서비스 옵션 REST API 컨트롤러
 */
@Tag(name = "무료 서비스 옵션", description = "무료 서비스(어메니티) 옵션 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/free-service-options")
@RequiredArgsConstructor
public class FreeServiceOptionApiController {

    private final AccessControlService accessControlService;
    private final FreeServiceOptionService freeServiceOptionService;

    @Operation(summary = "무료 옵션 목록 조회", description = "프로퍼티 무료 서비스 옵션 전체 목록")
    @GetMapping
    public ResponseEntity<HolaResponse<List<FreeServiceOptionResponse>>> getFreeServiceOptions(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<FreeServiceOptionResponse> list = freeServiceOptionService.getFreeServiceOptions(propertyId);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @Operation(summary = "무료 옵션 상세 조회", description = "무료 서비스 옵션 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<FreeServiceOptionResponse>> getFreeServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(freeServiceOptionService.getFreeServiceOption(id)));
    }

    @Operation(summary = "무료 옵션 등록", description = "새 무료 서비스 옵션 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<FreeServiceOptionResponse>> createFreeServiceOption(
            @PathVariable Long propertyId,
            @Valid @RequestBody FreeServiceOptionCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        FreeServiceOptionResponse response = freeServiceOptionService.createFreeServiceOption(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "무료 옵션 수정", description = "무료 서비스 옵션 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<FreeServiceOptionResponse>> updateFreeServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody FreeServiceOptionUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(freeServiceOptionService.updateFreeServiceOption(id, request)));
    }

    @Operation(summary = "무료 옵션 삭제", description = "무료 서비스 옵션 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteFreeServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        freeServiceOptionService.deleteFreeServiceOption(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "옵션코드 중복 확인", description = "프로퍼티 내 무료 서비스 옵션코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String serviceOptionCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = freeServiceOptionService.existsServiceOptionCode(propertyId, serviceOptionCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
