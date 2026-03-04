package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.hotel.dto.request.MarketCodeRequest;
import com.hola.hotel.dto.response.MarketCodeResponse;
import com.hola.hotel.service.MarketCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/market-codes")
@RequiredArgsConstructor
public class MarketCodeApiController {

    private final MarketCodeService marketCodeService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<MarketCodeResponse>>> getMarketCodes(@PathVariable Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(marketCodeService.getMarketCodes(propertyId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<MarketCodeResponse>> getMarketCode(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(marketCodeService.getMarketCode(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<MarketCodeResponse>> createMarketCode(
            @PathVariable Long propertyId, @Valid @RequestBody MarketCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(marketCodeService.createMarketCode(propertyId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<MarketCodeResponse>> updateMarketCode(
            @PathVariable Long id, @Valid @RequestBody MarketCodeRequest request) {
        return ResponseEntity.ok(HolaResponse.success(marketCodeService.updateMarketCode(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteMarketCode(@PathVariable Long id) {
        marketCodeService.deleteMarketCode(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 마켓코드명 중복 확인 */
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkMarketCode(
            @PathVariable Long propertyId,
            @RequestParam String marketCode) {
        boolean duplicate = marketCodeService.existsMarketCode(propertyId, marketCode);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
