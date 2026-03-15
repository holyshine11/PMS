package com.hola.common.auth.controller;

import com.hola.common.auth.dto.BluewaveAdminCreateRequest;
import com.hola.common.auth.dto.BluewaveAdminListResponse;
import com.hola.common.auth.dto.BluewaveAdminResponse;
import com.hola.common.auth.dto.BluewaveAdminUpdateRequest;
import com.hola.common.auth.service.BluewaveAdminService;
import com.hola.common.dto.HolaResponse;
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
 * 블루웨이브 관리자 REST API
 */
@Tag(name = "블루웨이브 관리자", description = "블루웨이브(슈퍼) 관리자 계정 관리 API")
@RestController
@RequestMapping("/api/v1/bluewave-admins")
@RequiredArgsConstructor
public class BluewaveAdminApiController {

    private final BluewaveAdminService bluewaveAdminService;

    @Operation(summary = "관리자 목록 조회", description = "블루웨이브 관리자 목록 (아이디/이름/사용여부 필터)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<BluewaveAdminListResponse>>> getList(
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Boolean useYn) {
        List<BluewaveAdminListResponse> list = bluewaveAdminService.getList(loginId, userName, useYn);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @Operation(summary = "관리자 상세 조회", description = "블루웨이브 관리자 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<BluewaveAdminResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(bluewaveAdminService.getDetail(id)));
    }

    @Operation(summary = "관리자 등록", description = "블루웨이브 관리자 계정 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<BluewaveAdminResponse>> create(
            @Valid @RequestBody BluewaveAdminCreateRequest request) {
        BluewaveAdminResponse response = bluewaveAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "관리자 수정", description = "블루웨이브 관리자 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<BluewaveAdminResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BluewaveAdminUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(bluewaveAdminService.update(id, request)));
    }

    @Operation(summary = "관리자 삭제", description = "블루웨이브 관리자 계정 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(@PathVariable Long id) {
        bluewaveAdminService.delete(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "아이디 중복 확인", description = "로그인 아이디 중복 여부 조회")
    @GetMapping("/check-login-id")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkLoginId(
            @RequestParam String loginId) {
        boolean duplicate = bluewaveAdminService.checkLoginId(loginId);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    @Operation(summary = "비밀번호 초기화", description = "관리자 비밀번호를 초기값으로 재설정")
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<HolaResponse<Void>> resetPassword(@PathVariable Long id) {
        bluewaveAdminService.resetPassword(id);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
