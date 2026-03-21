package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.hotel.dto.request.HousekeeperCreateRequest;
import com.hola.hotel.dto.request.HousekeeperUpdateRequest;
import com.hola.hotel.dto.response.HousekeeperResponse;
import com.hola.hotel.service.HousekeeperService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "하우스키퍼 목록", description = "하우스키퍼 담당자 목록 조회")
    @GetMapping
    public ResponseEntity<HolaResponse<List<HousekeeperResponse>>> getList(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(housekeeperService.getList(propertyId)));
    }

    @Operation(summary = "하우스키퍼 상세", description = "하우스키퍼 담당자 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<HousekeeperResponse>> getDetail(
            @PathVariable Long propertyId, @PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(housekeeperService.getDetail(propertyId, id)));
    }

    @Operation(summary = "하우스키퍼 등록", description = "하우스키퍼 담당자 등록")
    @PostMapping
    public ResponseEntity<HolaResponse<HousekeeperResponse>> create(
            @PathVariable Long propertyId,
            @Valid @RequestBody HousekeeperCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(housekeeperService.create(propertyId, request)));
    }

    @Operation(summary = "하우스키퍼 수정", description = "하우스키퍼 담당자 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<HousekeeperResponse>> update(
            @PathVariable Long propertyId, @PathVariable Long id,
            @RequestBody HousekeeperUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(housekeeperService.update(propertyId, id, request)));
    }

    @Operation(summary = "하우스키퍼 삭제", description = "하우스키퍼 담당자 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(
            @PathVariable Long propertyId, @PathVariable Long id) {
        housekeeperService.delete(propertyId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "비밀번호 초기화", description = "하우스키퍼 비밀번호 초기화")
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<HolaResponse<Void>> resetPassword(
            @PathVariable Long propertyId, @PathVariable Long id) {
        housekeeperService.resetPassword(propertyId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "비밀번호 변경", description = "하우스키퍼 비밀번호 변경")
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

    @Operation(summary = "아이디 중복 확인", description = "하우스키퍼 로그인 ID 중복 확인")
    @GetMapping("/check-login-id")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkLoginId(
            @PathVariable Long propertyId, @RequestParam String loginId) {
        boolean available = housekeeperService.checkLoginIdAvailable(loginId);
        return ResponseEntity.ok(HolaResponse.success(Map.of("available", available)));
    }
}
