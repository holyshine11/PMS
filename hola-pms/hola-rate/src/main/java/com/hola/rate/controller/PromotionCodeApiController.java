package com.hola.rate.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.rate.dto.request.PromotionCodeCreateRequest;
import com.hola.rate.dto.request.PromotionCodeUpdateRequest;
import com.hola.rate.dto.response.PromotionCodeListResponse;
import com.hola.rate.dto.response.PromotionCodeResponse;
import com.hola.rate.service.PromotionCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "프로모션 코드", description = "프로모션(할인) 코드 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/promotion-codes")
@RequiredArgsConstructor
public class PromotionCodeApiController {

    private final AccessControlService accessControlService;
    private final PromotionCodeService promotionCodeService;

    @Operation(summary = "프로모션코드 목록 조회", description = "프로퍼티 프로모션코드 전체 목록")
    @GetMapping
    public ResponseEntity<HolaResponse<List<PromotionCodeListResponse>>> getPromotionCodes(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(promotionCodeService.getPromotionCodes(propertyId)));
    }

    @Operation(summary = "프로모션코드 상세 조회", description = "프로모션코드 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<PromotionCodeResponse>> getPromotionCode(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(promotionCodeService.getPromotionCode(id)));
    }

    @Operation(summary = "프로모션코드 등록", description = "새 프로모션코드 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<PromotionCodeResponse>> createPromotionCode(
            @PathVariable Long propertyId, @Valid @RequestBody PromotionCodeCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(promotionCodeService.createPromotionCode(propertyId, request)));
    }

    @Operation(summary = "프로모션코드 수정", description = "프로모션코드 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<PromotionCodeResponse>> updatePromotionCode(
            @PathVariable Long propertyId, @PathVariable Long id,
            @Valid @RequestBody PromotionCodeUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(promotionCodeService.updatePromotionCode(id, request)));
    }

    @Operation(summary = "프로모션코드 삭제", description = "프로모션코드 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deletePromotionCode(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        promotionCodeService.deletePromotionCode(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "프로모션코드 중복 확인", description = "프로퍼티 내 프로모션코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId, @RequestParam String promotionCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = promotionCodeService.existsPromotionCode(propertyId, promotionCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
