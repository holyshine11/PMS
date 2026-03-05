package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HotelAdminCreateRequest;
import com.hola.hotel.dto.request.HotelAdminUpdateRequest;
import com.hola.hotel.dto.response.HotelAdminListResponse;
import com.hola.hotel.dto.response.HotelAdminResponse;
import com.hola.hotel.service.HotelAdminService;
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
@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/admins")
@RequiredArgsConstructor
public class HotelAdminApiController {

    private final HotelAdminService hotelAdminService;
    private final AccessControlService accessControlService;

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

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelAdminResponse>> getDetail(
            @PathVariable Long hotelId,
            @PathVariable Long id) {
        accessControlService.validateHotelAccess(hotelId);
        return ResponseEntity.ok(HolaResponse.success(hotelAdminService.getDetail(hotelId, id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<HotelAdminResponse>> create(
            @PathVariable Long hotelId,
            @Valid @RequestBody HotelAdminCreateRequest request) {
        accessControlService.validateHotelAccess(hotelId);
        HotelAdminResponse response = hotelAdminService.create(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelAdminResponse>> update(
            @PathVariable Long hotelId,
            @PathVariable Long id,
            @Valid @RequestBody HotelAdminUpdateRequest request) {
        accessControlService.validateHotelAccess(hotelId);
        return ResponseEntity.ok(HolaResponse.success(hotelAdminService.update(hotelId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(
            @PathVariable Long hotelId,
            @PathVariable Long id) {
        accessControlService.validateHotelAccess(hotelId);
        hotelAdminService.delete(hotelId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 아이디 중복 확인 */
    @GetMapping("/check-login-id")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkLoginId(
            @PathVariable Long hotelId,
            @RequestParam String loginId) {
        accessControlService.validateHotelAccess(hotelId);
        boolean duplicate = hotelAdminService.checkLoginId(loginId);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    /** 비밀번호 초기화 */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<HolaResponse<Void>> resetPassword(
            @PathVariable Long hotelId,
            @PathVariable Long id) {
        accessControlService.validateHotelAccess(hotelId);
        hotelAdminService.resetPassword(hotelId, id);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
