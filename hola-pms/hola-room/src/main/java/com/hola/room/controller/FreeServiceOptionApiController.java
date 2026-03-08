package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.FreeServiceOptionCreateRequest;
import com.hola.room.dto.request.FreeServiceOptionUpdateRequest;
import com.hola.room.dto.response.FreeServiceOptionResponse;
import com.hola.room.service.FreeServiceOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 무료 서비스 옵션 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/free-service-options")
@RequiredArgsConstructor
public class FreeServiceOptionApiController {

    private final AccessControlService accessControlService;
    private final FreeServiceOptionService freeServiceOptionService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<FreeServiceOptionResponse>>> getFreeServiceOptions(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<FreeServiceOptionResponse> list = freeServiceOptionService.getFreeServiceOptions(propertyId);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<FreeServiceOptionResponse>> getFreeServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(freeServiceOptionService.getFreeServiceOption(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<FreeServiceOptionResponse>> createFreeServiceOption(
            @PathVariable Long propertyId,
            @Valid @RequestBody FreeServiceOptionCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        FreeServiceOptionResponse response = freeServiceOptionService.createFreeServiceOption(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<FreeServiceOptionResponse>> updateFreeServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody FreeServiceOptionUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(freeServiceOptionService.updateFreeServiceOption(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteFreeServiceOption(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        freeServiceOptionService.deleteFreeServiceOption(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String serviceOptionCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = freeServiceOptionService.existsServiceOptionCode(propertyId, serviceOptionCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }
}
