package com.hola.hotel.service;

import com.hola.hotel.dto.request.RoleCreateRequest;
import com.hola.hotel.dto.request.RoleUpdateRequest;
import com.hola.hotel.dto.response.MenuTreeResponse;
import com.hola.hotel.dto.response.RoleListResponse;
import com.hola.hotel.dto.response.RoleResponse;

import java.util.List;
import java.util.Map;

/**
 * 프로퍼티 관리자 권한 서비스
 */
public interface PropertyRoleService {

    List<RoleListResponse> getList(Long hotelId, String roleName, Boolean useYn);

    RoleResponse getDetail(Long id);

    RoleResponse create(RoleCreateRequest request);

    RoleResponse update(Long id, RoleUpdateRequest request);

    void delete(Long id);

    Map<String, Boolean> checkName(Long hotelId, Long propertyId, String roleName, Long excludeId);

    List<MenuTreeResponse> getMenuTree();

    List<RoleListResponse> getRolesForSelector(Long hotelId, Long propertyId);

    Map<String, Boolean> checkPropertyHasRole(Long propertyId);
}
