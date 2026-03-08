package com.hola.rate.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.rate.dto.request.PromotionCodeCreateRequest;
import com.hola.rate.dto.request.PromotionCodeUpdateRequest;
import com.hola.rate.dto.response.PromotionCodeListResponse;
import com.hola.rate.dto.response.PromotionCodeResponse;
import com.hola.rate.service.PromotionCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/promotion-codes")
@RequiredArgsConstructor
public class PromotionCodeApiController {

    private final AccessControlService accessControlService;
    private final PromotionCodeService promotionCodeService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<PromotionCodeListResponse>>> getPromotionCodes(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(promotionCodeService.getPromotionCodes(propertyId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<PromotionCodeResponse>> getPromotionCode(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(promotionCodeService.getPromotionCode(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<PromotionCodeResponse>> createPromotionCode(
            @PathVariable Long propertyId, @Valid @RequestBody PromotionCodeCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(promotionCodeService.createPromotionCode(propertyId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<PromotionCodeResponse>> updatePromotionCode(
            @PathVariable Long propertyId, @PathVariable Long id,
            @Valid @RequestBody PromotionCodeUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(promotionCodeService.updatePromotionCode(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deletePromotionCode(
            @PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        promotionCodeService.deletePromotionCode(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId, @RequestParam String promotionCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = promotionCodeService.existsPromotionCode(propertyId, promotionCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
