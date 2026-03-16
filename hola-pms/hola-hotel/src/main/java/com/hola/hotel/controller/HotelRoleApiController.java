package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.hotel.dto.request.RoleCreateRequest;
import com.hola.hotel.dto.request.RoleUpdateRequest;
import com.hola.hotel.dto.response.MenuTreeResponse;
import com.hola.hotel.dto.response.RoleListResponse;
import com.hola.hotel.dto.response.RoleResponse;
import com.hola.hotel.service.HotelRoleService;
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
 * 호텔 관리자 권한 REST API
 */
@Tag(name = "호텔 관리자 권한", description = "호텔 관리자 역할 CRUD, 메뉴 권한 트리 API")
@RestController
@RequestMapping("/api/v1/hotel-admin-roles")
@RequiredArgsConstructor
public class HotelRoleApiController {

    private final HotelRoleService hotelRoleService;

    /** 목록 조회 */
    @Operation(summary = "권한 목록 조회", description = "호텔 관리자 권한 목록 (호텔/권한명/사용여부 필터)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<RoleListResponse>>> getList(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean useYn) {
        return ResponseEntity.ok(HolaResponse.success(hotelRoleService.getList(hotelId, roleName, useYn)));
    }

    /** 상세 조회 */
    @Operation(summary = "권한 상세 조회", description = "권한 ID로 상세 정보 + 메뉴 권한 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoleResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(hotelRoleService.getDetail(id)));
    }

    /** 등록 */
    @Operation(summary = "권한 등록", description = "호텔 관리자 권한 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<RoleResponse>> create(@Valid @RequestBody RoleCreateRequest request) {
        RoleResponse response = hotelRoleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    /** 수정 */
    @Operation(summary = "권한 수정", description = "호텔 관리자 권한 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(hotelRoleService.update(id, request)));
    }

    /** 삭제 */
    @Operation(summary = "권한 삭제", description = "호텔 관리자 권한 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(@PathVariable Long id) {
        hotelRoleService.delete(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 권한명 중복확인 */
    @Operation(summary = "권한명 중복 확인", description = "호텔 내 권한명 중복 여부 조회")
    @GetMapping("/check-name")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkName(
            @RequestParam Long hotelId,
            @RequestParam String roleName,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(HolaResponse.success(hotelRoleService.checkName(hotelId, roleName, excludeId)));
    }

    /** 메뉴 트리 (HOTEL_ADMIN) */
    @Operation(summary = "메뉴 트리 조회", description = "호텔 관리자용 3-depth 메뉴 권한 트리")
    @GetMapping("/menu-tree")
    public ResponseEntity<HolaResponse<List<MenuTreeResponse>>> getMenuTree() {
        return ResponseEntity.ok(HolaResponse.success(hotelRoleService.getMenuTree()));
    }

    /** 권한 선택 목록 (드롭다운용, useYn=true만) */
    @Operation(summary = "권한 선택 목록", description = "드롭다운용 권한 목록 (활성화만)")
    @GetMapping("/selector")
    public ResponseEntity<HolaResponse<List<RoleListResponse>>> getRolesForSelector(
            @RequestParam(required = false) Long hotelId) {
        if (hotelId == null) {
            return ResponseEntity.ok(HolaResponse.success(java.util.Collections.emptyList()));
        }
        return ResponseEntity.ok(HolaResponse.success(hotelRoleService.getRolesForSelector(hotelId)));
    }
}
