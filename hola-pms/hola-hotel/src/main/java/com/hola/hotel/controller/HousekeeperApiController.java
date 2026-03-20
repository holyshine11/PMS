package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.hotel.dto.request.HousekeeperCreateRequest;
import com.hola.hotel.dto.request.HousekeeperUpdateRequest;
import com.hola.hotel.dto.response.HousekeeperResponse;
import com.hola.hotel.service.HousekeeperService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 하우스키퍼 담당자 관리 REST API
 */
@Tag(name = "하우스키퍼 담당자 관리")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/housekeepers")
@RequiredArgsConstructor
public class HousekeeperApiController {

    private final HousekeeperService housekeeperService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<HousekeeperResponse>>> getList(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(housekeeperService.getList(propertyId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<HousekeeperResponse>> getDetail(
            @PathVariable Long propertyId, @PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(housekeeperService.getDetail(propertyId, id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<HousekeeperResponse>> create(
            @PathVariable Long propertyId,
            @Valid @RequestBody HousekeeperCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(housekeeperService.create(propertyId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<HousekeeperResponse>> update(
            @PathVariable Long propertyId, @PathVariable Long id,
            @RequestBody HousekeeperUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(housekeeperService.update(propertyId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(
            @PathVariable Long propertyId, @PathVariable Long id) {
        housekeeperService.delete(propertyId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<HolaResponse<Void>> resetPassword(
            @PathVariable Long propertyId, @PathVariable Long id) {
        housekeeperService.resetPassword(propertyId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<HolaResponse<Void>> changePassword(
            @PathVariable Long propertyId, @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 10) {
            return ResponseEntity.badRequest()
                    .body(HolaResponse.error("HOLA-0802", "비밀번호는 10자 이상이어야 합니다."));
        }
        housekeeperService.changePassword(propertyId, id, newPassword);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @GetMapping("/check-login-id")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkLoginId(
            @PathVariable Long propertyId, @RequestParam String loginId) {
        boolean available = housekeeperService.checkLoginIdAvailable(loginId);
        return ResponseEntity.ok(HolaResponse.success(Map.of("available", available)));
    }
}
