package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HkCleaningPolicyRequest;
import com.hola.hotel.dto.response.HkCleaningPolicyResponse;
import com.hola.hotel.service.HkCleaningPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 청소 정책 관리 REST API
 */
@Tag(name = "청소 정책 관리")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/hk-cleaning-policies")
@RequiredArgsConstructor
public class HkCleaningPolicyApiController {

    private final HkCleaningPolicyService cleaningPolicyService;
    private final AccessControlService accessControlService;

    @Operation(summary = "전체 정책 목록", description = "모든 룸타입의 정책 (오버라이드 여부 표시)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<HkCleaningPolicyResponse>>> getAllPolicies(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(cleaningPolicyService.getAllPolicies(propertyId)));
    }

    @Operation(summary = "정책 생성/수정", description = "룸타입별 오버라이드 설정 (null 필드 = 기본값 사용)")
    @PostMapping
    public ResponseEntity<HolaResponse<HkCleaningPolicyResponse>> createOrUpdate(
            @PathVariable Long propertyId,
            @RequestBody HkCleaningPolicyRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                cleaningPolicyService.createOrUpdate(propertyId, request)));
    }

    @Operation(summary = "오버라이드 삭제", description = "룸타입 오버라이드 제거 (프로퍼티 기본값으로 복귀)")
    @DeleteMapping("/{roomTypeId}")
    public ResponseEntity<HolaResponse<Void>> deletePolicy(
            @PathVariable Long propertyId, @PathVariable Long roomTypeId) {
        accessControlService.validatePropertyAccess(propertyId);
        cleaningPolicyService.deletePolicy(propertyId, roomTypeId);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
