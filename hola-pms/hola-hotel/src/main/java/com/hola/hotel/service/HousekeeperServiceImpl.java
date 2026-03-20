package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.entity.AdminUserProperty;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HousekeeperCreateRequest;
import com.hola.hotel.dto.request.HousekeeperUpdateRequest;
import com.hola.hotel.dto.response.HousekeeperResponse;
import com.hola.hotel.entity.HkSection;
import com.hola.hotel.entity.HkSectionHousekeeper;
import com.hola.hotel.repository.HkSectionRepository;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 하우스키퍼 담당자 관리 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousekeeperServiceImpl implements HousekeeperService {

    private static final String DEFAULT_PASSWORD = "holapms1!";
    private static final Set<String> HK_ROLES = Set.of("HOUSEKEEPER", "HOUSEKEEPING_SUPERVISOR");

    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final PropertyRepository propertyRepository;
    private final HkSectionRepository hkSectionRepository;
    private final AccessControlService accessControlService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<HousekeeperResponse> getList(Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);

        // 프로퍼티에 매핑된 사용자 ID 조회
        List<Long> userIds = adminUserPropertyRepository.findAdminUserIdsByPropertyId(propertyId);
        if (userIds.isEmpty()) return List.of();

        // 하우스키퍼 → 구역 매핑
        Map<Long, String> sectionMap = buildHkSectionMap(propertyId);

        return userIds.stream()
                .map(id -> adminUserRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .filter(u -> HK_ROLES.contains(u.getRole()))
                .map(u -> toResponseWithSection(u, sectionMap.get(u.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public HousekeeperResponse getDetail(Long propertyId, Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        AdminUser user = findHousekeeperById(id);
        Map<Long, String> sectionMap = buildHkSectionMap(propertyId);
        return toResponseWithSection(user, sectionMap.get(user.getId()));
    }

    @Override
    @Transactional
    public HousekeeperResponse create(Long propertyId, HousekeeperCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);

        // 아이디 중복 확인
        if (adminUserRepository.existsByLoginIdAndDeletedAtIsNull(request.getLoginId())) {
            throw new HolaException(ErrorCode.ADMIN_LOGIN_ID_DUPLICATE);
        }

        // 역할 검증
        String role = request.getRole() != null ? request.getRole() : "HOUSEKEEPER";
        if (!HK_ROLES.contains(role)) {
            throw new HolaException(ErrorCode.INVALID_INPUT);
        }

        // 프로퍼티의 호텔 ID 조회
        Long hotelId = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND))
                .getHotel().getId();

        // 계정 생성
        AdminUser user = AdminUser.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .userName(request.getUserName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(role)
                .accountType("HOTEL_ADMIN")
                .hotelId(hotelId)
                .department(request.getDepartment() != null ? request.getDepartment() : "하우스키핑")
                .position(request.getPosition())
                .build();

        AdminUser saved = adminUserRepository.save(user);

        // 프로퍼티 매핑
        AdminUserProperty mapping = AdminUserProperty.builder()
                .adminUserId(saved.getId())
                .propertyId(propertyId)
                .build();
        adminUserPropertyRepository.save(mapping);

        log.info("하우스키퍼 등록: {} ({}, {})", saved.getUserName(), saved.getLoginId(), role);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public HousekeeperResponse update(Long propertyId, Long id, HousekeeperUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        AdminUser user = findHousekeeperById(id);

        // 역할 변경 시 검증
        String newRole = request.getRole() != null ? request.getRole() : user.getRole();
        if (!HK_ROLES.contains(newRole)) {
            throw new HolaException(ErrorCode.INVALID_INPUT);
        }

        user.updateProfile(
                request.getUserName() != null ? request.getUserName() : user.getUserName(),
                request.getEmail() != null ? request.getEmail() : user.getEmail(),
                request.getPhone() != null ? request.getPhone() : user.getPhone(),
                user.getMobileCountryCode(),
                user.getMobile(),
                user.getPhoneCountryCode(),
                request.getDepartment() != null ? request.getDepartment() : user.getDepartment(),
                request.getPosition() != null ? request.getPosition() : user.getPosition(),
                null, // roleName
                null, // roleId
                request.getUseYn()
        );

        // 역할 변경은 별도 처리 (updateProfile에서 role 필드를 변경하지 않으므로)
        // AdminUser에 직접 role 변경 메서드가 없으므로 리플렉션 대신 네이티브 쿼리 사용하지 않고
        // 현재 구조에서는 역할 변경을 지원하지 않음 (등록 시 결정)

        log.info("하우스키퍼 수정: {} ({})", user.getUserName(), user.getLoginId());
        return toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long propertyId, Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        AdminUser user = findHousekeeperById(id);
        user.softDelete();
        log.info("하우스키퍼 삭제: {} ({})", user.getUserName(), user.getLoginId());
    }

    @Override
    @Transactional
    public void resetPassword(Long propertyId, Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        AdminUser user = findHousekeeperById(id);
        user.resetPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        log.info("하우스키퍼 비밀번호 초기화: {} ({})", user.getUserName(), user.getLoginId());
    }

    @Override
    @Transactional
    public void changePassword(Long propertyId, Long id, String newPassword) {
        accessControlService.validatePropertyAccess(propertyId);
        AdminUser user = findHousekeeperById(id);
        user.resetPassword(passwordEncoder.encode(newPassword));
        log.info("하우스키퍼 비밀번호 변경: {} ({})", user.getUserName(), user.getLoginId());
    }

    @Override
    public boolean checkLoginIdAvailable(String loginId) {
        return !adminUserRepository.existsByLoginIdAndDeletedAtIsNull(loginId);
    }

    // === Private ===

    private AdminUser findHousekeeperById(Long id) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));
        if (!HK_ROLES.contains(user.getRole())) {
            throw new HolaException(ErrorCode.ADMIN_NOT_FOUND);
        }
        return user;
    }

    /** 하우스키퍼 → 소속 구역명 매핑 */
    private Map<Long, String> buildHkSectionMap(Long propertyId) {
        Map<Long, String> result = new HashMap<>();
        List<HkSection> sections = hkSectionRepository.findByPropertyIdOrderBySortOrder(propertyId);
        for (HkSection section : sections) {
            for (HkSectionHousekeeper shk : section.getHousekeepers()) {
                result.put(shk.getHousekeeperId(), section.getSectionName());
            }
        }
        return result;
    }

    private HousekeeperResponse toResponseWithSection(AdminUser user, String sectionName) {
        HousekeeperResponse response = toResponse(user);
        return HousekeeperResponse.builder()
                .id(response.getId())
                .loginId(response.getLoginId())
                .userName(response.getUserName())
                .email(response.getEmail())
                .phone(response.getPhone())
                .department(response.getDepartment())
                .position(response.getPosition())
                .role(response.getRole())
                .roleLabel(response.getRoleLabel())
                .useYn(response.getUseYn())
                .createdAt(response.getCreatedAt())
                .sectionName(sectionName)
                .build();
    }

    private HousekeeperResponse toResponse(AdminUser user) {
        String roleLabel;
        switch (user.getRole()) {
            case "HOUSEKEEPING_SUPERVISOR": roleLabel = "감독자"; break;
            case "HOUSEKEEPER": roleLabel = "청소 담당"; break;
            default: roleLabel = user.getRole();
        }

        return HousekeeperResponse.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .position(user.getPosition())
                .role(user.getRole())
                .roleLabel(roleLabel)
                .useYn(user.getUseYn())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
