package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.entity.AdminUserProperty;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.PropertyAdminCreateRequest;
import com.hola.hotel.dto.request.PropertyAdminUpdateRequest;
import com.hola.hotel.dto.response.PropertyAdminListResponse;
import com.hola.hotel.dto.response.PropertyAdminResponse;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyAdminServiceImpl implements PropertyAdminService {

    private static final String DEFAULT_PASSWORD = "holapms1!";
    private static final String ACCOUNT_TYPE = "PROPERTY_ADMIN";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final PropertyRepository propertyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PropertyAdminListResponse> getList(Long propertyId, String loginId, String userName, Boolean useYn) {
        // propertyId로 매핑된 adminUserIds 추출
        Set<Long> adminUserIds = adminUserPropertyRepository.findByPropertyId(propertyId).stream()
                .map(AdminUserProperty::getAdminUserId)
                .collect(Collectors.toSet());

        if (adminUserIds.isEmpty()) {
            return List.of();
        }

        // AdminUser 조회 후 Java 필터링 (JPQL null 조건부 LIKE → PostgreSQL bytea 캐스팅 이슈 방지)
        List<AdminUser> admins = adminUserRepository.findAllById(adminUserIds).stream()
                .filter(a -> a.getDeletedAt() == null)
                .filter(a -> ACCOUNT_TYPE.equals(a.getAccountType()))
                .filter(a -> loginId == null || loginId.isEmpty() || a.getLoginId().contains(loginId))
                .filter(a -> userName == null || userName.isEmpty() || a.getUserName().contains(userName))
                .filter(a -> useYn == null || a.getUseYn().equals(useYn))
                .collect(Collectors.toList());

        return admins.stream().map(admin -> PropertyAdminListResponse.builder()
                .id(admin.getId())
                .loginId(maskLoginId(admin.getLoginId()))
                .userName(maskUserName(admin.getUserName()))
                .useYn(admin.getUseYn())
                .createdAt(admin.getCreatedAt() != null ? admin.getCreatedAt().format(DATE_FORMAT) : "")
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public PropertyAdminResponse getDetail(Long propertyId, Long id) {
        AdminUser admin = findAdminById(id, propertyId);
        Property property = findPropertyById(propertyId);

        return PropertyAdminResponse.builder()
                .id(admin.getId())
                .memberNumber(admin.getMemberNumber())
                .loginId(admin.getLoginId())
                .userName(admin.getUserName())
                .email(admin.getEmail())
                .phone(admin.getPhone())
                .phoneCountryCode(admin.getPhoneCountryCode())
                .mobileCountryCode(admin.getMobileCountryCode())
                .mobile(admin.getMobile())
                .department(admin.getDepartment())
                .position(admin.getPosition())
                .roleName(admin.getRoleName())
                .accountType("프로퍼티 관리자")
                .useYn(admin.getUseYn())
                .hotelId(property.getHotel().getId())
                .hotelName(property.getHotel().getHotelName())
                .propertyId(propertyId)
                .propertyName(property.getPropertyName())
                .createdAt(admin.getCreatedAt() != null ? admin.getCreatedAt().format(DATE_FORMAT) : "")
                .build();
    }

    @Override
    @Transactional
    public PropertyAdminResponse create(Long propertyId, PropertyAdminCreateRequest request) {
        // 아이디 중복 체크
        if (adminUserRepository.existsByLoginIdAndDeletedAtIsNull(request.getLoginId())) {
            throw new HolaException(ErrorCode.ADMIN_LOGIN_ID_DUPLICATE);
        }

        Property property = findPropertyById(propertyId);
        String memberNumber = generateMemberNumber();

        // 관리자 생성
        AdminUser admin = AdminUser.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .userName(request.getUserName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .phoneCountryCode(request.getPhoneCountryCode())
                .mobileCountryCode(request.getMobileCountryCode())
                .mobile(request.getMobile())
                .department(request.getDepartment())
                .position(request.getPosition())
                .roleName(request.getRoleName())
                .role("PROPERTY_ADMIN")
                .memberNumber(memberNumber)
                .accountType(ACCOUNT_TYPE)
                .hotelId(property.getHotel().getId())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            admin.deactivate();
        }

        AdminUser saved = adminUserRepository.save(admin);

        // 단일 프로퍼티 매핑 저장
        AdminUserProperty mapping = AdminUserProperty.builder()
                .adminUserId(saved.getId())
                .propertyId(propertyId)
                .build();
        adminUserPropertyRepository.save(mapping);

        log.info("프로퍼티 관리자 생성: {} ({})", saved.getUserName(), saved.getLoginId());
        return getDetail(propertyId, saved.getId());
    }

    @Override
    @Transactional
    public PropertyAdminResponse update(Long propertyId, Long id, PropertyAdminUpdateRequest request) {
        AdminUser admin = findAdminById(id, propertyId);

        admin.updateProfile(
                request.getUserName(), request.getEmail(), request.getPhone(),
                request.getMobileCountryCode(), request.getMobile(),
                request.getPhoneCountryCode(),
                request.getDepartment(), request.getPosition(),
                request.getRoleName(), request.getUseYn());

        log.info("프로퍼티 관리자 수정: {} ({})", admin.getUserName(), admin.getLoginId());
        return getDetail(propertyId, admin.getId());
    }

    @Override
    @Transactional
    public void delete(Long propertyId, Long id) {
        AdminUser admin = findAdminById(id, propertyId);
        admin.softDelete();
        adminUserPropertyRepository.deleteByAdminUserId(admin.getId());
        log.info("프로퍼티 관리자 삭제: {} ({})", admin.getUserName(), admin.getLoginId());
    }

    @Override
    public boolean checkLoginId(String loginId) {
        return adminUserRepository.existsByLoginIdAndDeletedAtIsNull(loginId);
    }

    @Override
    @Transactional
    public void resetPassword(Long propertyId, Long id) {
        AdminUser admin = findAdminById(id, propertyId);
        admin.resetPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        log.info("프로퍼티 관리자 비밀번호 초기화: {} ({})", admin.getUserName(), admin.getLoginId());
    }

    // === private 메서드 ===

    private AdminUser findAdminById(Long id, Long propertyId) {
        AdminUser admin = adminUserRepository.findById(id)
                .filter(a -> a.getDeletedAt() == null)
                .filter(a -> ACCOUNT_TYPE.equals(a.getAccountType()))
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));

        // 해당 프로퍼티에 매핑되어 있는지 확인
        boolean mapped = adminUserPropertyRepository.findByAdminUserId(admin.getId()).stream()
                .anyMatch(p -> propertyId.equals(p.getPropertyId()));
        if (!mapped) {
            throw new HolaException(ErrorCode.ADMIN_NOT_FOUND);
        }

        return admin;
    }

    private Property findPropertyById(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));
    }

    private String generateMemberNumber() {
        Long nextVal = adminUserRepository.getNextMemberNumberSequence();
        return String.format("U%09d", nextVal);
    }

    /**
     * 아이디 마스킹: 앞 5자 + *****
     */
    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.length() <= 5) return loginId;
        return loginId.substring(0, 5) + "*****";
    }

    /**
     * 이름 마스킹: 성 + * + 끝자
     */
    private String maskUserName(String userName) {
        if (userName == null || userName.length() < 2) return userName;
        if (userName.length() == 2) {
            return userName.charAt(0) + "*";
        }
        return userName.charAt(0) + "*" + userName.charAt(userName.length() - 1);
    }
}
