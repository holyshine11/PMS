package com.hola.hotel.service;

import com.hola.common.auth.entity.Menu;
import com.hola.common.auth.entity.Role;
import com.hola.common.auth.entity.RoleMenu;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.auth.repository.MenuRepository;
import com.hola.common.auth.repository.RoleMenuRepository;
import com.hola.common.auth.repository.RoleRepository;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.RoleCreateRequest;
import com.hola.hotel.dto.request.RoleUpdateRequest;
import com.hola.hotel.dto.response.MenuTreeResponse;
import com.hola.hotel.dto.response.RoleListResponse;
import com.hola.hotel.dto.response.RoleResponse;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.HotelRepository;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 프로퍼티 관리자 권한 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyRoleServiceImpl implements PropertyRoleService {

    private static final String TARGET_TYPE = "PROPERTY_ADMIN";
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;
    private final AdminUserRepository adminUserRepository;
    private final HotelRepository hotelRepository;
    private final PropertyRepository propertyRepository;
    private final AccessControlService accessControlService;

    @Override
    public List<RoleListResponse> getList(Long hotelId, String roleName, Boolean useYn) {
        // HOTEL_ADMIN: 자기 호텔 권한만 조회 가능
        Long effectiveHotelId = enforceHotelScope(hotelId);

        List<Role> roles = roleRepository.findByTargetTypeAndDeletedAtIsNullOrderBySortOrderAsc(TARGET_TYPE)
                .stream()
                .filter(r -> effectiveHotelId == null || effectiveHotelId.equals(r.getHotelId()))
                .filter(r -> roleName == null || roleName.isEmpty() || r.getRoleName().contains(roleName))
                .filter(r -> useYn == null || useYn.equals(r.getUseYn()))
                .collect(Collectors.toList());

        Map<Long, String> hotelNameMap = hotelRepository.findAll().stream()
                .collect(Collectors.toMap(Hotel::getId, Hotel::getHotelName, (a, b) -> a));
        Map<Long, String> propertyNameMap = propertyRepository.findAll().stream()
                .collect(Collectors.toMap(Property::getId, Property::getPropertyName, (a, b) -> a));

        return roles.stream().map(role -> RoleListResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .hotelId(role.getHotelId())
                .hotelName(hotelNameMap.getOrDefault(role.getHotelId(), ""))
                .propertyId(role.getPropertyId())
                .propertyName(propertyNameMap.getOrDefault(role.getPropertyId(), ""))
                .useYn(role.getUseYn())
                .updatedAt(role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMAT)
                        : role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMAT) : "")
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public RoleResponse getDetail(Long id) {
        Role role = findRoleById(id);
        validateHotelAccess(role.getHotelId());
        List<RoleMenu> mappings = roleMenuRepository.findByRoleId(id);
        List<Long> menuIds = mappings.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());

        String hotelName = hotelRepository.findById(role.getHotelId())
                .map(Hotel::getHotelName).orElse("");
        String propertyName = role.getPropertyId() != null
                ? propertyRepository.findById(role.getPropertyId()).map(Property::getPropertyName).orElse("")
                : "";

        return RoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .hotelId(role.getHotelId())
                .hotelName(hotelName)
                .propertyId(role.getPropertyId())
                .propertyName(propertyName)
                .useYn(role.getUseYn())
                .updatedAt(role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMAT)
                        : role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMAT) : "")
                .menuIds(menuIds)
                .build();
    }

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        validateHotelAccess(request.getHotelId());

        // 해당 프로퍼티에 이미 권한이 존재하는지 확인
        if (roleRepository.existsByPropertyIdAndTargetTypeAndDeletedAtIsNull(
                request.getPropertyId(), TARGET_TYPE)) {
            throw new HolaException(ErrorCode.ROLE_PROPERTY_ALREADY_EXISTS);
        }

        if (roleRepository.existsByRoleNameAndHotelIdAndPropertyIdAndDeletedAtIsNull(
                request.getRoleName(), request.getHotelId(), request.getPropertyId())) {
            throw new HolaException(ErrorCode.ROLE_NAME_DUPLICATE);
        }

        Role role = Role.builder()
                .roleName(request.getRoleName())
                .hotelId(request.getHotelId())
                .propertyId(request.getPropertyId())
                .targetType(TARGET_TYPE)
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            role.deactivate();
        }

        Role saved = roleRepository.save(role);
        saveMenuMappings(saved.getId(), request.getMenuIds());

        log.info("프로퍼티 관리자 권한 생성: {} (hotelId={}, propertyId={})", saved.getRoleName(), saved.getHotelId(), saved.getPropertyId());
        return getDetail(saved.getId());
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = findRoleById(id);
        validateHotelAccess(role.getHotelId());

        if (roleRepository.existsByRoleNameAndHotelIdAndPropertyIdAndIdNotAndDeletedAtIsNull(
                request.getRoleName(), role.getHotelId(), role.getPropertyId(), id)) {
            throw new HolaException(ErrorCode.ROLE_NAME_DUPLICATE);
        }

        role.update(request.getRoleName(), request.getUseYn());

        roleMenuRepository.deleteByRoleId(id);
        saveMenuMappings(id, request.getMenuIds());

        log.info("프로퍼티 관리자 권한 수정: {} (id={})", role.getRoleName(), id);
        return getDetail(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = findRoleById(id);
        validateHotelAccess(role.getHotelId());

        if (adminUserRepository.existsByRoleIdAndDeletedAtIsNull(id)) {
            throw new HolaException(ErrorCode.ROLE_HAS_USERS);
        }

        role.softDelete();
        roleMenuRepository.deleteByRoleId(id);
        log.info("프로퍼티 관리자 권한 삭제: {} (id={})", role.getRoleName(), id);
    }

    @Override
    public Map<String, Boolean> checkName(Long hotelId, Long propertyId, String roleName, Long excludeId) {
        validateHotelAccess(hotelId);
        boolean duplicate;
        if (excludeId != null) {
            duplicate = roleRepository.existsByRoleNameAndHotelIdAndPropertyIdAndIdNotAndDeletedAtIsNull(roleName, hotelId, propertyId, excludeId);
        } else {
            duplicate = roleRepository.existsByRoleNameAndHotelIdAndPropertyIdAndDeletedAtIsNull(roleName, hotelId, propertyId);
        }
        return Map.of("duplicate", duplicate);
    }

    @Override
    public List<MenuTreeResponse> getMenuTree() {
        List<Menu> menus = menuRepository.findByTargetTypeAndUseYnTrueAndDeletedAtIsNullOrderBySortOrderAsc(TARGET_TYPE);

        Map<Long, List<Menu>> childrenMap = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(Menu::getParentId));

        return menus.stream()
                .filter(m -> m.getDepth() == 1)
                .map(parent -> buildTree(parent, childrenMap))
                .collect(Collectors.toList());
    }

    private MenuTreeResponse buildTree(Menu menu, Map<Long, List<Menu>> childrenMap) {
        List<MenuTreeResponse> children = childrenMap.getOrDefault(menu.getId(), Collections.emptyList())
                .stream()
                .map(child -> buildTree(child, childrenMap))
                .collect(Collectors.toList());

        return MenuTreeResponse.builder()
                .id(menu.getId())
                .menuCode(menu.getMenuCode())
                .menuName(menu.getMenuName())
                .depth(menu.getDepth())
                .children(children)
                .build();
    }

    @Override
    public List<RoleListResponse> getRolesForSelector(Long hotelId, Long propertyId) {
        validateHotelAccess(hotelId);
        return roleRepository.findByTargetTypeAndDeletedAtIsNullOrderBySortOrderAsc(TARGET_TYPE)
                .stream()
                .filter(r -> hotelId.equals(r.getHotelId()))
                .filter(r -> propertyId == null || propertyId.equals(r.getPropertyId()))
                .filter(Role::getUseYn)
                .map(role -> RoleListResponse.builder()
                        .id(role.getId())
                        .roleName(role.getRoleName())
                        .hotelId(role.getHotelId())
                        .propertyId(role.getPropertyId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Boolean> checkPropertyHasRole(Long propertyId) {
        boolean exists = roleRepository.existsByPropertyIdAndTargetTypeAndDeletedAtIsNull(propertyId, TARGET_TYPE);
        return Map.of("exists", exists);
    }

    /**
     * HOTEL_ADMIN인 경우 자기 호텔 ID를 강제 적용 (목록 조회용)
     * SUPER_ADMIN은 파라미터 그대로 사용
     */
    private Long enforceHotelScope(Long requestedHotelId) {
        AdminUser currentUser = accessControlService.getCurrentUser();
        if (currentUser.isSuperAdmin()) {
            return requestedHotelId;
        }
        // HOTEL_ADMIN: 항상 자기 호텔로 강제
        return currentUser.getHotelId();
    }

    /**
     * HOTEL_ADMIN인 경우 해당 호텔에 대한 접근 권한 검증
     */
    private void validateHotelAccess(Long hotelId) {
        accessControlService.validateHotelAccess(hotelId);
    }

    private Role findRoleById(Long id) {
        return roleRepository.findById(id)
                .filter(r -> r.getDeletedAt() == null)
                .filter(r -> TARGET_TYPE.equals(r.getTargetType()))
                .orElseThrow(() -> new HolaException(ErrorCode.ROLE_NOT_FOUND));
    }

    private void saveMenuMappings(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) return;
        List<RoleMenu> mappings = menuIds.stream()
                .map(menuId -> RoleMenu.builder()
                        .roleId(roleId)
                        .menuId(menuId)
                        .build())
                .collect(Collectors.toList());
        roleMenuRepository.saveAll(mappings);
    }
}
