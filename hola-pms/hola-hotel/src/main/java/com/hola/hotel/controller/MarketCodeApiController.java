package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.MarketCodeRequest;
import com.hola.hotel.dto.response.MarketCodeResponse;
import com.hola.hotel.service.MarketCodeService;
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

@Tag(name = "마켓코드", description = "프로퍼티 마켓코드 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/market-codes")
@RequiredArgsConstructor
public class MarketCodeApiController {

    private final MarketCodeService marketCodeService;
    private final AccessControlService accessControlService;

    @Operation(summary = "마켓코드 목록 조회", description = "프로퍼티 마켓코드 전체 목록")
    @GetMapping
    public ResponseEntity<HolaResponse<List<MarketCodeResponse>>> getMarketCodes(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(marketCodeService.getMarketCodes(propertyId)));
    }

    @Operation(summary = "마켓코드 상세 조회", description = "마켓코드 ID로 상세 정보 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<MarketCodeResponse>> getMarketCode(@PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(marketCodeService.getMarketCode(id)));
    }

    @Operation(summary = "마켓코드 등록", description = "새 마켓코드 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<MarketCodeResponse>> createMarketCode(
            @PathVariable Long propertyId, @Valid @RequestBody MarketCodeRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(marketCodeService.createMarketCode(propertyId, request)));
    }

    @Operation(summary = "마켓코드 수정", description = "마켓코드 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<MarketCodeResponse>> updateMarketCode(
            @PathVariable Long propertyId, @PathVariable Long id, @Valid @RequestBody MarketCodeRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(marketCodeService.updateMarketCode(id, request)));
    }

    @Operation(summary = "마켓코드 삭제", description = "마켓코드 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteMarketCode(@PathVariable Long propertyId, @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        marketCodeService.deleteMarketCode(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 마켓코드명 중복 확인 */
    @Operation(summary = "마켓코드 중복 확인", description = "프로퍼티 내 마켓코드 중복 여부 조회")
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkMarketCode(
            @PathVariable Long propertyId,
            @RequestParam String marketCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = marketCodeService.existsMarketCode(propertyId, marketCode);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
