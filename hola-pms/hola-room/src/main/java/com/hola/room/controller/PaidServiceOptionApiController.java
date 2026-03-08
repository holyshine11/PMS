package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.PaidServiceOptionCreateRequest;
import com.hola.room.dto.request.PaidServiceOptionUpdateRequest;
import com.hola.room.dto.response.PaidServiceOptionResponse;
import com.hola.room.service.PaidServiceOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 유료 서비스 옵션 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/paid-service-options")
@RequiredArgsConstructor
public class PaidServiceOptionApiController {

    private final AccessControlService accessControlService;
    private final PaidServiceOptionService paidServiceOptionService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<PaidServiceOptionResponse>>> getPaidServiceOptions(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<PaidServiceOptionResponse> list = paidServiceOptionService.getPaidServiceOptions(propertyId);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<PaidServiceOptionResponse>> getPaidServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(paidServiceOptionService.getPaidServiceOption(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<PaidServiceOptionResponse>> createPaidServiceOption(
            @PathVariable Long propertyId,
            @Valid @RequestBody PaidServiceOptionCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        PaidServiceOptionResponse response = paidServiceOptionService.createPaidServiceOption(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<PaidServiceOptionResponse>> updatePaidServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody PaidServiceOptionUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(paidServiceOptionService.updatePaidServiceOption(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deletePaidServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        paidServiceOptionService.deletePaidServiceOption(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String serviceOptionCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = paidServiceOptionService.existsServiceOptionCode(propertyId, serviceOptionCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
