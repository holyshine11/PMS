package com.hola.rate.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.rate.dto.request.DayUseRateRequest;
import com.hola.rate.dto.request.RateCodeCreateRequest;
import com.hola.rate.dto.request.RateCodeUpdateRequest;
import com.hola.rate.dto.request.RatePricingRequest;
import com.hola.rate.dto.response.DayUseRateResponse;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.dto.response.RateCodeResponse;
import com.hola.rate.dto.response.RatePricingResponse;
import com.hola.rate.entity.DayUseRate;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.rate.service.RateCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "레이트 코드", description = "레이트 코드(요금제) 관리 및 요금정보 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/rate-codes")
@RequiredArgsConstructor
public class RateCodeApiController {

    private final AccessControlService accessControlService;
    private final RateCodeService rateCodeService;
    private final DayUseRateRepository dayUseRateRepository;

    @Operation(summary = "레이트코드 목록 조회", description = "프로퍼티 레이트코드 전체 목록 (체크인/아웃 기간 필터 가능, dayUseEnabled=false 시 Dayuse 제외)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<RateCodeListResponse>>> getRateCodes(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "true") boolean dayUseEnabled) {
        accessControlService.validatePropertyAccess(propertyId);
        List<RateCodeListResponse> list;
        if (checkIn != null && checkOut != null) {
            // 체크인~체크아웃 기간을 요금으로 100% 커버하는 레이트코드만 반환
            list = rateCodeService.getAvailableRateCodes(propertyId, checkIn, checkOut, dayUseEnabled);
        } else {
            list = rateCodeService.getRateCodes(propertyId);
        }
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @Operation(summary = "레이트코드 상세 조회", description = "레이트코드 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RateCodeResponse>> getRateCode(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.getRateCode(id)));
    }

    @Operation(summary = "레이트코드 등록", description = "새 레이트코드 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<RateCodeResponse>> createRateCode(
            @PathVariable Long propertyId,
            @Valid @RequestBody RateCodeCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        RateCodeResponse response = rateCodeService.createRateCode(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "레이트코드 수정", description = "레이트코드 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RateCodeResponse>> updateRateCode(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody RateCodeUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.updateRateCode(id, request)));
    }

    @Operation(summary = "레이트코드 삭제", description = "레이트코드 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRateCode(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        rateCodeService.deleteRateCode(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "레이트코드 중복 확인", description = "프로퍼티 내 레이트코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String rateCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = rateCodeService.existsRateCode(propertyId, rateCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    // ===== 요금정보 API =====

    @Operation(summary = "요금정보 조회", description = "레이트코드의 날짜별/객실타입별 요금 매트릭스 조회")
    @GetMapping("/{id}/pricing")
    public ResponseEntity<HolaResponse<RatePricingResponse>> getRatePricing(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.getRatePricing(id)));
    }

    @Operation(summary = "요금정보 저장", description = "레이트코드 요금 매트릭스 저장")
    @PostMapping("/{id}/pricing")
    public ResponseEntity<HolaResponse<RatePricingResponse>> saveRatePricing(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody RatePricingRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.saveRatePricing(id, request)));
    }

    @Operation(summary = "요금정보 행 삭제", description = "요금 매트릭스 특정 행 삭제")
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

    @Operation(summary = "옵션요금 조회", description = "레이트코드에 연결된 유료 서비스 옵션 ID 목록 조회")
    @GetMapping("/{id}/option-pricing")
    public ResponseEntity<HolaResponse<List<Long>>> getOptionPricing(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.getOptionPricing(id)));
    }

    // ===== Dayuse 요금 API =====

    @Operation(summary = "Dayuse 요금 조회", description = "레이트코드의 Dayuse 시간별 요금 목록")
    @GetMapping("/{id}/dayuse-rates")
    public ResponseEntity<HolaResponse<List<DayUseRateResponse>>> getDayUseRates(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        List<DayUseRateResponse> rates = dayUseRateRepository.findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(id)
                .stream().map(r -> DayUseRateResponse.builder()
                        .id(r.getId()).rateCodeId(r.getRateCodeId())
                        .durationHours(r.getDurationHours()).supplyPrice(r.getSupplyPrice())
                        .description(r.getDescription()).sortOrder(r.getSortOrder()).useYn(r.getUseYn())
                        .build()).toList();
        return ResponseEntity.ok(HolaResponse.success(rates));
    }

    @Operation(summary = "Dayuse 요금 등록", description = "Dayuse 시간별 요금 추가")
    @PostMapping("/{id}/dayuse-rates")
    public ResponseEntity<HolaResponse<DayUseRateResponse>> createDayUseRate(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody DayUseRateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        DayUseRate rate = DayUseRate.builder()
                .rateCodeId(id)
                .durationHours(request.getDurationHours())
                .supplyPrice(request.getSupplyPrice())
                .description(request.getDescription())
                .build();
        rate = dayUseRateRepository.save(rate);
        DayUseRateResponse resp = DayUseRateResponse.builder()
                .id(rate.getId()).rateCodeId(rate.getRateCodeId())
                .durationHours(rate.getDurationHours()).supplyPrice(rate.getSupplyPrice())
                .description(rate.getDescription()).sortOrder(rate.getSortOrder()).useYn(rate.getUseYn())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(resp));
    }

    @Operation(summary = "Dayuse 요금 삭제", description = "Dayuse 시간별 요금 soft delete")
    @DeleteMapping("/{id}/dayuse-rates/{rateId}")
    public ResponseEntity<HolaResponse<Void>> deleteDayUseRate(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @PathVariable Long rateId) {
        accessControlService.validatePropertyAccess(propertyId);
        dayUseRateRepository.findById(rateId).ifPresent(r -> {
            r.softDelete();
            dayUseRateRepository.save(r);
        });
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "옵션요금 저장", description = "레이트코드에 유료 서비스 옵션 연결 저장")
    @PostMapping("/{id}/option-pricing")
    public ResponseEntity<HolaResponse<List<Long>>> saveOptionPricing(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @RequestBody List<Long> paidServiceOptionIds) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(rateCodeService.saveOptionPricing(id, paidServiceOptionIds)));
    }
}
