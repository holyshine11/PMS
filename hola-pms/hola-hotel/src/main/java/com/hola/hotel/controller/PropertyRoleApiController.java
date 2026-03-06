package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.hotel.dto.request.RoleCreateRequest;
import com.hola.hotel.dto.request.RoleUpdateRequest;
import com.hola.hotel.dto.response.MenuTreeResponse;
import com.hola.hotel.dto.response.RoleListResponse;
import com.hola.hotel.dto.response.RoleResponse;
import com.hola.hotel.service.PropertyRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프로퍼티 관리자 권한 REST API
 */
@RestController
@RequestMapping("/api/v1/property-admin-roles")
@RequiredArgsConstructor
public class PropertyRoleApiController {

    private final PropertyRoleService propertyRoleService;

    /** 목록 조회 */
    @GetMapping
    public ResponseEntity<HolaResponse<List<RoleListResponse>>> getList(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean useYn) {
        return ResponseEntity.ok(HolaResponse.success(propertyRoleService.getList(hotelId, roleName, useYn)));
    }

    /** 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoleResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(propertyRoleService.getDetail(id)));
    }

    /** 등록 */
    @PostMapping
    public ResponseEntity<HolaResponse<RoleResponse>> create(@Valid @RequestBody RoleCreateRequest request) {
        RoleResponse response = propertyRoleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    /** 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(propertyRoleService.update(id, request)));
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(@PathVariable Long id) {
        propertyRoleService.delete(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 권한명 중복확인 */
    @GetMapping("/check-name")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkName(
            @RequestParam Long hotelId,
            @RequestParam(required = false) Long propertyId,
            @RequestParam String roleName,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(HolaResponse.success(propertyRoleService.checkName(hotelId, propertyId, roleName, excludeId)));
    }

    /** 프로퍼티 권한 존재 여부 확인 */
    @GetMapping("/check-property")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkPropertyHasRole(
            @RequestParam Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(propertyRoleService.checkPropertyHasRole(propertyId)));
    }

    /** 메뉴 트리 (PROPERTY_ADMIN) */
    @GetMapping("/menu-tree")
    public ResponseEntity<HolaResponse<List<MenuTreeResponse>>> getMenuTree() {
        return ResponseEntity.ok(HolaResponse.success(propertyRoleService.getMenuTree()));
    }

    /** 권한 선택 목록 (드롭다운용, useYn=true만) */
    @GetMapping("/selector")
    public ResponseEntity<HolaResponse<List<RoleListResponse>>> getRolesForSelector(
            @RequestParam Long hotelId,
            @RequestParam(required = false) Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(propertyRoleService.getRolesForSelector(hotelId, propertyId)));
    }
}
