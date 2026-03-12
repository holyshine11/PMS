package com.hola.rate.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.rate.dto.request.RateCodeCreateRequest;
import com.hola.rate.dto.request.RateCodeUpdateRequest;
import com.hola.rate.dto.request.RatePricingRequest;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.dto.response.RateCodeResponse;
import com.hola.rate.dto.response.RatePricingResponse;
import com.hola.rate.service.RateCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 레이트 코드 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/rate-codes")
@RequiredArgsConstructor
public class RateCodeApiController {

    private final AccessControlService accessControlService;
    private final RateCodeService rateCodeService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<RateCodeListResponse>>> getRateCodes(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        accessControlService.validatePropertyAccess(propertyId);
        List<RateCodeListResponse> list;
        if (checkIn != null && checkOut != null) {
            // 체크인~체크아웃 기간을 요금으로 100% 커버하는 레이트코드만 반환
            list = rateCodeService.getAvailableRateCodes(propertyId, checkIn, checkOut);
        } else {
            list = rateCodeService.getRateCodes(propertyId);
        }
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RateCodeResponse>> getRateCode(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.getRateCode(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<RateCodeResponse>> createRateCode(
            @PathVariable Long propertyId,
            @Valid @RequestBody RateCodeCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        RateCodeResponse response = rateCodeService.createRateCode(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RateCodeResponse>> updateRateCode(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody RateCodeUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.updateRateCode(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRateCode(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        rateCodeService.deleteRateCode(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String rateCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = rateCodeService.existsRateCode(propertyId, rateCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    // ===== 요금정보 API =====

    @GetMapping("/{id}/pricing")
    public ResponseEntity<HolaResponse<RatePricingResponse>> getRatePricing(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.getRatePricing(id)));
    }

    @PostMapping("/{id}/pricing")
    public ResponseEntity<HolaResponse<RatePricingResponse>> saveRatePricing(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @RequestBody RatePricingRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.saveRatePricing(id, request)));
    }

    @DeleteMapping("/{id}/pricing/{pricingId}")
    public ResponseEntity<HolaResponse<Void>> deleteRatePricingRow(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @PathVariable Long pricingId) {
        accessControlService.validatePropertyAccess(propertyId);
        rateCodeService.deleteRatePricingRow(id, pricingId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // ===== 옵션요금 API =====

    @GetMapping("/{id}/option-pricing")
    public ResponseEntity<HolaResponse<List<Long>>> getOptionPricing(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.getOptionPricing(id)));
    }

    @PostMapping("/{id}/option-pricing")
    public ResponseEntity<HolaResponse<List<Long>>> saveOptionPricing(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @RequestBody List<Long> paidServiceOptionIds) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.saveOptionPricing(id, paidServiceOptionIds)));
    }
}
