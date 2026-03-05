package com.hola.common.auth.controller;

import com.hola.common.auth.dto.BluewaveAdminCreateRequest;
import com.hola.common.auth.dto.BluewaveAdminListResponse;
import com.hola.common.auth.dto.BluewaveAdminResponse;
import com.hola.common.auth.dto.BluewaveAdminUpdateRequest;
import com.hola.common.auth.service.BluewaveAdminService;
import com.hola.common.dto.HolaResponse;
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
@RestController
@RequestMapping("/api/v1/bluewave-admins")
@RequiredArgsConstructor
public class BluewaveAdminApiController {

    private final BluewaveAdminService bluewaveAdminService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<BluewaveAdminListResponse>>> getList(
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Boolean useYn) {
        List<BluewaveAdminListResponse> list = bluewaveAdminService.getList(loginId, userName, useYn);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<BluewaveAdminResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(bluewaveAdminService.getDetail(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<BluewaveAdminResponse>> create(
            @Valid @RequestBody BluewaveAdminCreateRequest request) {
        BluewaveAdminResponse response = bluewaveAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<BluewaveAdminResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BluewaveAdminUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(bluewaveAdminService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(@PathVariable Long id) {
        bluewaveAdminService.delete(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 아이디 중복 확인 */
    @GetMapping("/check-login-id")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkLoginId(
            @RequestParam String loginId) {
        boolean duplicate = bluewaveAdminService.checkLoginId(loginId);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    /** 비밀번호 초기화 */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<HolaResponse<Void>> resetPassword(@PathVariable Long id) {
        bluewaveAdminService.resetPassword(id);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
