package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.PropertyAdminCreateRequest;
import com.hola.hotel.dto.request.PropertyAdminUpdateRequest;
import com.hola.hotel.dto.response.PropertyAdminListResponse;
import com.hola.hotel.dto.response.PropertyAdminResponse;
import com.hola.hotel.service.PropertyAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프로퍼티 관리자 REST API
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/admins")
@RequiredArgsConstructor
public class PropertyAdminApiController {

    private final PropertyAdminService propertyAdminService;
    private final AccessControlService accessControlService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<PropertyAdminListResponse>>> getList(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Boolean useYn) {
        accessControlService.validatePropertyAccess(propertyId);
        List<PropertyAdminListResponse> list = propertyAdminService.getList(propertyId, loginId, userName, useYn);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<PropertyAdminResponse>> getDetail(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(propertyAdminService.getDetail(propertyId, id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<PropertyAdminResponse>> create(
            @PathVariable Long propertyId,
            @Valid @RequestBody PropertyAdminCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        PropertyAdminResponse response = propertyAdminService.create(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<PropertyAdminResponse>> update(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody PropertyAdminUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(propertyAdminService.update(propertyId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        propertyAdminService.delete(propertyId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 아이디 중복 확인 */
    @GetMapping("/check-login-id")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkLoginId(
            @PathVariable Long propertyId,
            @RequestParam String loginId) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = propertyAdminService.checkLoginId(loginId);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    /** 비밀번호 초기화 */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<HolaResponse<Void>> resetPassword(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        propertyAdminService.resetPassword(propertyId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
