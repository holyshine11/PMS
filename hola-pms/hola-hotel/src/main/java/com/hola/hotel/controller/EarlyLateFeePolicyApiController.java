package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.EarlyLateFeePolicySaveRequest;
import com.hola.hotel.dto.response.EarlyLateFeePolicyResponse;
import com.hola.hotel.service.EarlyLateFeePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 얼리 체크인 / 레이트 체크아웃 요금 정책 API
 */
@Tag(name = "얼리/레이트 요금 정책", description = "얼리 체크인, 레이트 체크아웃 요금 정책 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/early-late-policies")
@RequiredArgsConstructor
public class EarlyLateFeePolicyApiController {

    private final EarlyLateFeePolicyService earlyLateFeePolicyService;
    private final AccessControlService accessControlService;

    /**
     * 전체 조회 (optional policyType 필터)
     */
    @Operation(summary = "요금 정책 조회", description = "얼리 체크인/레이트 체크아웃 요금 정책 목록 (타입 필터 가능)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<EarlyLateFeePolicyResponse>>> getEarlyLateFeePolicies(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String policyType) {
        accessControlService.validatePropertyAccess(propertyId);

        List<EarlyLateFeePolicyResponse> result;
        if (policyType != null && !policyType.isBlank()) {
            result = earlyLateFeePolicyService.getEarlyLateFeePoliciesByType(propertyId, policyType);
        } else {
            result = earlyLateFeePolicyService.getEarlyLateFeePolicies(propertyId);
        }
        return ResponseEntity.ok(HolaResponse.success(result));
    }

    /**
     * 전체 저장 (기존 soft delete + 재등록)
     */
    @Operation(summary = "요금 정책 저장", description = "요금 정책 전체 저장 (기존 삭제 후 재등록)")
    @PostMapping
    public ResponseEntity<HolaResponse<List<EarlyLateFeePolicyResponse>>> saveEarlyLateFeePolicies(
            @PathVariable Long propertyId,
            @Valid @RequestBody EarlyLateFeePolicySaveRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                earlyLateFeePolicyService.saveEarlyLateFeePolicies(propertyId, request)));
    }
}
