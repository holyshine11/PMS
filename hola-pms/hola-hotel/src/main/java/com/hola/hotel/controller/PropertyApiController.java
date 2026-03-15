package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.dto.PageInfo;
import com.hola.hotel.dto.request.CancellationFeeSaveRequest;
import com.hola.hotel.dto.request.PropertyCreateRequest;
import com.hola.hotel.dto.request.PropertySettlementRequest;
import com.hola.hotel.dto.request.PropertyTaxServiceChargeRequest;
import com.hola.hotel.dto.request.PropertyUpdateRequest;
import com.hola.hotel.dto.response.CancellationFeeResponse;
import com.hola.hotel.dto.response.PropertyResponse;
import com.hola.hotel.dto.response.PropertySettlementResponse;
import com.hola.hotel.service.CancellationFeeService;
import com.hola.hotel.service.PropertyService;
import com.hola.hotel.service.PropertySettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.hola.common.security.AccessControlService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 프로퍼티 REST API
 */
@Tag(name = "프로퍼티 관리", description = "프로퍼티 CRUD, 정산정보, 취소수수료, TAX/봉사료 API")
@RestController
@RequiredArgsConstructor
public class PropertyApiController {

    private final PropertyService propertyService;
    private final PropertySettlementService settlementService;
    private final AccessControlService accessControlService;
    private final CancellationFeeService cancellationFeeService;

    /** 호텔 하위 프로퍼티 목록 */
    @Operation(summary = "프로퍼티 목록 조회", description = "호텔 하위 프로퍼티 목록 (페이징)")
    @GetMapping("/api/v1/hotels/{hotelId}/properties")
    public ResponseEntity<HolaResponse<List<PropertyResponse>>> getProperties(
            @PathVariable Long hotelId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PropertyResponse> page = propertyService.getProperties(hotelId, pageable);
        return ResponseEntity.ok(
                HolaResponse.success(page.getContent(), PageInfo.from(page)));
    }

    /** 프로퍼티 단건 조회 */
    @Operation(summary = "프로퍼티 상세 조회", description = "프로퍼티 ID로 상세 정보 조회")
    @GetMapping("/api/v1/properties/{id}")
    public ResponseEntity<HolaResponse<PropertyResponse>> getProperty(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(propertyService.getProperty(id)));
    }

    /** 프로퍼티 생성 */
    @Operation(summary = "프로퍼티 등록", description = "호텔 하위 프로퍼티 생성")
    @PostMapping("/api/v1/hotels/{hotelId}/properties")
    public ResponseEntity<HolaResponse<PropertyResponse>> createProperty(
            @PathVariable Long hotelId,
            @Valid @RequestBody PropertyCreateRequest request) {
        PropertyResponse response = propertyService.createProperty(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    /** 프로퍼티 수정 */
    @Operation(summary = "프로퍼티 수정", description = "프로퍼티 정보 수정")
    @PutMapping("/api/v1/properties/{id}")
    public ResponseEntity<HolaResponse<PropertyResponse>> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(propertyService.updateProperty(id, request)));
    }

    /** 프로퍼티 삭제 */
    @Operation(summary = "프로퍼티 삭제", description = "프로퍼티 소프트 삭제")
    @DeleteMapping("/api/v1/properties/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 프로퍼티명 중복확인 */
    @Operation(summary = "프로퍼티명 중복 확인", description = "호텔 내 프로퍼티명 중복 여부 조회")
    @GetMapping("/api/v1/hotels/{hotelId}/properties/check-name")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkPropertyName(
            @PathVariable Long hotelId,
            @RequestParam String propertyName) {
        boolean duplicate = propertyService.existsPropertyName(hotelId, propertyName);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }

    // ─── 정산정보 API ────────────────────────────

    /** 프로퍼티 정산정보 전체 조회 */
    @Operation(summary = "정산정보 조회", description = "프로퍼티 정산정보 전체 조회")
    @GetMapping("/api/v1/properties/{propertyId}/settlements")
    public ResponseEntity<HolaResponse<List<PropertySettlementResponse>>> getSettlements(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(settlementService.getSettlements(propertyId)));
    }

    /** 프로퍼티 정산정보 저장 (Upsert) */
    @Operation(summary = "정산정보 저장", description = "프로퍼티 정산정보 저장 (Upsert)")
    @PutMapping("/api/v1/properties/{propertyId}/settlements")
    public ResponseEntity<HolaResponse<PropertySettlementResponse>> saveSettlement(
            @PathVariable Long propertyId,
            @Valid @RequestBody PropertySettlementRequest request) {
        return ResponseEntity.ok(HolaResponse.success(settlementService.saveSettlement(propertyId, request)));
    }

    /** 프로퍼티 정산정보 삭제 */
    @Operation(summary = "정산정보 삭제", description = "프로퍼티 정산정보 삭제")
    @DeleteMapping("/api/v1/properties/{propertyId}/settlements/{countryType}")
    public ResponseEntity<HolaResponse<Void>> deleteSettlement(
            @PathVariable Long propertyId,
            @PathVariable String countryType) {
        settlementService.deleteSettlement(propertyId, countryType);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // ─── 취소 수수료 API ────────────────────────────

    /** 프로퍼티 취소 수수료 목록 조회 */
    @Operation(summary = "취소 수수료 조회", description = "프로퍼티 취소 수수료 목록 조회")
    @GetMapping("/api/v1/properties/{propertyId}/cancellation-fees")
    public ResponseEntity<HolaResponse<List<CancellationFeeResponse>>> getCancellationFees(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(cancellationFeeService.getCancellationFees(propertyId)));
    }

    /** 프로퍼티 취소 수수료 저장 (Replace-All) */
    @Operation(summary = "취소 수수료 저장", description = "프로퍼티 취소 수수료 전체 저장 (Replace-All)")
    @PutMapping("/api/v1/properties/{propertyId}/cancellation-fees")
    public ResponseEntity<HolaResponse<List<CancellationFeeResponse>>> saveCancellationFees(
            @PathVariable Long propertyId,
            @Valid @RequestBody CancellationFeeSaveRequest request) {
        return ResponseEntity.ok(HolaResponse.success(cancellationFeeService.saveCancellationFees(propertyId, request)));
    }

    // ─── TAX/봉사료 API ────────────────────────────

    /** TAX/봉사료 정보 저장 */
    @Operation(summary = "TAX/봉사료 저장", description = "프로퍼티 TAX/봉사료 정보 저장")
    @PutMapping("/api/v1/properties/{propertyId}/tax-service-charge")
    public ResponseEntity<HolaResponse<PropertyResponse>> saveTaxServiceCharge(
            @PathVariable Long propertyId,
            @Valid @RequestBody PropertyTaxServiceChargeRequest request) {
        return ResponseEntity.ok(HolaResponse.success(propertyService.updateTaxServiceCharge(propertyId, request)));
    }

    /** 헤더 드롭다운용 프로퍼티 목록 (로그인 사용자 권한 기반 필터링) */
    @Operation(summary = "프로퍼티 선택 목록", description = "헤더 드롭다운용 프로퍼티 목록 (권한 기반 필터링)")
    @GetMapping("/api/v1/properties/selector")
    public ResponseEntity<HolaResponse<List<PropertyResponse>>> getPropertiesForSelector(
            @RequestParam(required = false) Long hotelId) {
        // hotelId가 없으면 빈 배열 반환
        if (hotelId == null) {
            return ResponseEntity.ok(HolaResponse.success(Collections.emptyList()));
        }
        String loginId = accessControlService.getCurrentLoginId();
        return ResponseEntity.ok(HolaResponse.success(propertyService.getPropertiesForSelector(hotelId, loginId)));
    }
}
