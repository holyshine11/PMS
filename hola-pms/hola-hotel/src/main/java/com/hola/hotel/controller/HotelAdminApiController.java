package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HotelAdminCreateRequest;
import com.hola.hotel.dto.request.HotelAdminUpdateRequest;
import com.hola.hotel.dto.response.HotelAdminListResponse;
import com.hola.hotel.dto.response.HotelAdminResponse;
import com.hola.hotel.service.HotelAdminService;
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
 * 호텔 관리자 REST API
 */
@Tag(name = "호텔 관리자", description = "호텔 관리자 계정 CRUD, 아이디 중복확인, 비밀번호 초기화 API")
@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/admins")
@RequiredArgsConstructor
public class HotelAdminApiController {

    private final HotelAdminService hotelAdminService;
    private final AccessControlService accessControlService;

    @Operation(summary = "관리자 목록 조회", description = "호텔 관리자 목록 (아이디/이름/사용여부 필터)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<HotelAdminListResponse>>> getList(
            @PathVariable Long hotelId,
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Boolean useYn) {
        accessControlService.validateHotelAccess(hotelId);
        List<HotelAdminListResponse> list = hotelAdminService.getList(hotelId, loginId, userName, useYn);
        return ResponseEntity.ok(HolaResponse.success(list));
    }

    @Operation(summary = "관리자 상세 조회", description = "호텔 관리자 상세 정보")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelAdminResponse>> getDetail(
            @PathVariable Long hotelId,
            @PathVariable Long id) {
        accessControlService.validateHotelAccess(hotelId);
        return ResponseEntity.ok(HolaResponse.success(hotelAdminService.getDetail(hotelId, id)));
    }

    @Operation(summary = "관리자 등록", description = "호텔 관리자 계정 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<HotelAdminResponse>> create(
            @PathVariable Long hotelId,
            @Valid @RequestBody HotelAdminCreateRequest request) {
        accessControlService.validateHotelAccess(hotelId);
        HotelAdminResponse response = hotelAdminService.create(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "관리자 수정", description = "호텔 관리자 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelAdminResponse>> update(
            @PathVariable Long hotelId,
            @PathVariable Long id,
            @Valid @RequestBody HotelAdminUpdateRequest request) {
        accessControlService.validateHotelAccess(hotelId);
        return ResponseEntity.ok(HolaResponse.success(hotelAdminService.update(hotelId, id, request)));
    }

    @Operation(summary = "관리자 삭제", description = "호텔 관리자 계정 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(
            @PathVariable Long hotelId,
            @PathVariable Long id) {
        accessControlService.validateHotelAccess(hotelId);
        hotelAdminService.delete(hotelId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 아이디 중복 확인 */
    @Operation(summary = "아이디 중복 확인", description = "로그인 아이디 중복 여부 조회")
    @GetMapping("/check-login-id")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkLoginId(
            @PathVariable Long hotelId,
            @RequestParam String loginId) {
        accessControlService.validateHotelAccess(hotelId);
        boolean duplicate = hotelAdminService.checkLoginId(loginId);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    /** 비밀번호 초기화 */
    @Operation(summary = "비밀번호 초기화", description = "관리자 비밀번호를 초기값으로 재설정")
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<HolaResponse<Void>> resetPassword(
            @PathVariable Long hotelId,
            @PathVariable Long id) {
        accessControlService.validateHotelAccess(hotelId);
        hotelAdminService.resetPassword(hotelId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
