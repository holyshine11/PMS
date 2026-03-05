package com.hola.hotel.service;

import com.hola.common.auth.entity.Menu;
import com.hola.common.auth.entity.Role;
import com.hola.common.auth.entity.RoleMenu;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.auth.repository.MenuRepository;
import com.hola.common.auth.repository.RoleMenuRepository;
import com.hola.common.auth.repository.RoleRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.RoleCreateRequest;
import com.hola.hotel.dto.request.RoleUpdateRequest;
import com.hola.hotel.dto.response.MenuTreeResponse;
import com.hola.hotel.dto.response.RoleListResponse;
import com.hola.hotel.dto.response.RoleResponse;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelRoleServiceImpl implements HotelRoleService {

    private static final String TARGET_TYPE = "HOTEL_ADMIN";
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;
    private final AdminUserRepository adminUserRepository;
    private final HotelRepository hotelRepository;

    @Override
    public List<RoleListResponse> getList(Long hotelId, String roleName, Boolean useYn) {
        // 전체 조회 후 Java 필터링
        List<Role> roles = roleRepository.findByTargetTypeAndDeletedAtIsNullOrderBySortOrderAsc(TARGET_TYPE)
                .stream()
                .filter(r -> hotelId == null || hotelId.equals(r.getHotelId()))
                .filter(r -> roleName == null || roleName.isEmpty() || r.getRoleName().contains(roleName))
                .filter(r -> useYn == null || useYn.equals(r.getUseYn()))
                .collect(Collectors.toList());

        // 호텔명 매핑
        Map<Long, String> hotelNameMap = hotelRepository.findAll().stream()
                .collect(Collectors.toMap(Hotel::getId, Hotel::getHotelName, (a, b) -> a));

        return roles.stream().map(role -> RoleListResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .hotelId(role.getHotelId())
                .hotelName(hotelNameMap.getOrDefault(role.getHotelId(), ""))
                .useYn(role.getUseYn())
                .updatedAt(role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMAT)
                        : role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMAT) : "")
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public RoleResponse getDetail(Long id) {
        Role role = findRoleById(id);
        List<RoleMenu> mappings = roleMenuRepository.findByRoleId(id);
        List<Long> menuIds = mappings.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());

        String hotelName = hotelRepository.findById(role.getHotelId())
                .map(Hotel::getHotelName).orElse("");

        return RoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .hotelId(role.getHotelId())
                .hotelName(hotelName)
                .useYn(role.getUseYn())
                .updatedAt(role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMAT)
                        : role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMAT) : "")
                .menuIds(menuIds)
                .build();
    }

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        // 중복 확인
        if (roleRepository.existsByRoleNameAndHotelIdAndDeletedAtIsNull(
                request.getRoleName(), request.getHotelId())) {
            throw new HolaException(ErrorCode.ROLE_NAME_DUPLICATE);
        }

        Role role = Role.builder()
                .roleName(request.getRoleName())
                .hotelId(request.getHotelId())
                .targetType(TARGET_TYPE)
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            role.deactivate();
        }

        Role saved = roleRepository.save(role);

        // 메뉴 매핑
        saveMenuMappings(saved.getId(), request.getMenuIds());

        log.info("호텔 관리자 권한 생성: {} (hotelId={})", saved.getRoleName(), saved.getHotelId());
        return getDetail(saved.getId());
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = findRoleById(id);

        // 중복 확인 (자기 자신 제외)
        if (roleRepository.existsByRoleNameAndHotelIdAndIdNotAndDeletedAtIsNull(
                request.getRoleName(), role.getHotelId(), id)) {
            throw new HolaException(ErrorCode.ROLE_NAME_DUPLICATE);
        }

        role.update(request.getRoleName(), request.getUseYn());

        // 메뉴 매핑 갱신 (삭제 후 재등록)
        roleMenuRepository.deleteByRoleId(id);
        saveMenuMappings(id, request.getMenuIds());

        log.info("호텔 관리자 권한 수정: {} (id={})", role.getRoleName(), id);
        return getDetail(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = findRoleById(id);

        // 사용 중인 관리자 존재 확인
        if (adminUserRepository.existsByRoleIdAndDeletedAtIsNull(id)) {
            throw new HolaException(ErrorCode.ROLE_HAS_USERS);
        }

        role.softDelete();
        roleMenuRepository.deleteByRoleId(id);
        log.info("호텔 관리자 권한 삭제: {} (id={})", role.getRoleName(), id);
    }

    @Override
    public Map<String, Boolean> checkName(Long hotelId, String roleName, Long excludeId) {
        boolean duplicate;
        if (excludeId != null) {
            duplicate = roleRepository.existsByRoleNameAndHotelIdAndIdNotAndDeletedAtIsNull(roleName, hotelId, excludeId);
        } else {
            duplicate = roleRepository.existsByRoleNameAndHotelIdAndDeletedAtIsNull(roleName, hotelId);
        }
        return Map.of("duplicate", duplicate);
    }

    @Override
    public List<MenuTreeResponse> getMenuTree() {
        List<Menu> menus = menuRepository.findByTargetTypeAndUseYnTrueAndDeletedAtIsNullOrderBySortOrderAsc(TARGET_TYPE);

        // parentId로 그룹핑 (N-depth 재귀 지원)
        Map<Long, List<Menu>> childrenMap = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(Menu::getParentId));

        // 최상위(depth=1) 메뉴로 트리 빌드
        return menus.stream()
                .filter(m -> m.getDepth() == 1)
                .map(parent -> buildTree(parent, childrenMap))
                .collect(Collectors.toList());
    }

    /** 재귀적으로 메뉴 트리 구성 */
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
    public List<RoleListResponse> getRolesForSelector(Long hotelId) {
        return roleRepository.findByTargetTypeAndDeletedAtIsNullOrderBySortOrderAsc(TARGET_TYPE)
                .stream()
                .filter(r -> hotelId.equals(r.getHotelId()))
                .filter(Role::getUseYn)
                .map(role -> RoleListResponse.builder()
                        .id(role.getId())
                        .roleName(role.getRoleName())
                        .hotelId(role.getHotelId())
                        .build())
                .collect(Collectors.toList());
    }

    // === private ===

    private Role findRoleById(Long id) {
        return roleRepository.findById(id)
                .filter(r -> r.getDeletedAt() == null)
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
