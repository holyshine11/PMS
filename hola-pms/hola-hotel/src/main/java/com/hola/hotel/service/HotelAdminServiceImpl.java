package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.entity.AdminUserProperty;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.HotelAdminCreateRequest;
import com.hola.hotel.dto.request.HotelAdminUpdateRequest;
import com.hola.hotel.dto.response.HotelAdminListResponse;
import com.hola.hotel.dto.response.HotelAdminResponse;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.HotelRepository;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelAdminServiceImpl implements HotelAdminService {

    private static final String DEFAULT_PASSWORD = "holapms1!";
    private static final String ACCOUNT_TYPE = "HOTEL_ADMIN";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final HotelRepository hotelRepository;
    private final PropertyRepository propertyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<HotelAdminListResponse> getList(Long hotelId, String loginId, String userName, Boolean useYn) {
        // DB 조회 후 Java 필터링 (JPQL null 조건부 LIKE → PostgreSQL bytea 타입 캐스팅 이슈 방지)
        List<AdminUser> admins = adminUserRepository
                .findByHotelIdAndAccountTypeAndDeletedAtIsNullOrderByCreatedAtDesc(hotelId, ACCOUNT_TYPE)
                .stream()
                .filter(a -> loginId == null || loginId.isEmpty() || a.getLoginId().contains(loginId))
                .filter(a -> userName == null || userName.isEmpty() || a.getUserName().contains(userName))
                .filter(a -> useYn == null || a.getUseYn().equals(useYn))
                .collect(Collectors.toList());

        // 프로퍼티명 매핑을 위해 호텔의 전체 프로퍼티 로드
        Map<Long, String> propertyNameMap = propertyRepository.findAllByHotelId(hotelId).stream()
                .collect(Collectors.toMap(Property::getId, Property::getPropertyName));

        return admins.stream().map(admin -> {
            List<AdminUserProperty> props = adminUserPropertyRepository.findByAdminUserId(admin.getId());
            String propertyNames = props.stream()
                    .map(p -> propertyNameMap.getOrDefault(p.getPropertyId(), ""))
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.joining(", "));

            return HotelAdminListResponse.builder()
                    .id(admin.getId())
                    .loginId(maskLoginId(admin.getLoginId()))
                    .userName(maskUserName(admin.getUserName()))
                    .propertyNames(propertyNames)
                    .accountType("호텔 관리자")
                    .useYn(admin.getUseYn())
                    .createdAt(admin.getCreatedAt() != null ? admin.getCreatedAt().format(DATE_FORMAT) : "")
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public HotelAdminResponse getDetail(Long hotelId, Long id) {
        AdminUser admin = findAdminById(id, hotelId);
        List<AdminUserProperty> props = adminUserPropertyRepository.findByAdminUserId(admin.getId());
        List<Long> propertyIds = props.stream().map(AdminUserProperty::getPropertyId).collect(Collectors.toList());

        String hotelName = hotelRepository.findById(hotelId)
                .map(Hotel::getHotelName).orElse("");

        return HotelAdminResponse.builder()
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
                .roleId(admin.getRoleId())
                .accountType("호텔 관리자")
                .useYn(admin.getUseYn())
                .hotelId(hotelId)
                .hotelName(hotelName)
                .propertyIds(propertyIds)
                .createdAt(admin.getCreatedAt() != null ? admin.getCreatedAt().format(DATE_FORMAT) : "")
                .build();
    }

    @Override
    @Transactional
    public HotelAdminResponse create(Long hotelId, HotelAdminCreateRequest request) {
        // 아이디 중복 체크
        if (adminUserRepository.existsByLoginIdAndDeletedAtIsNull(request.getLoginId())) {
            throw new HolaException(ErrorCode.ADMIN_LOGIN_ID_DUPLICATE);
        }

        // 회원번호 생성
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
                .roleId(request.getRoleId())
                .role("HOTEL_ADMIN")
                .memberNumber(memberNumber)
                .accountType(ACCOUNT_TYPE)
                .hotelId(hotelId)
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            admin.deactivate();
        }

        AdminUser saved = adminUserRepository.save(admin);

        // 프로퍼티 매핑 저장
        savePropertyMappings(saved.getId(), request.getPropertyIds());

        log.info("호텔 관리자 생성: {} ({})", saved.getUserName(), saved.getLoginId());
        return getDetail(hotelId, saved.getId());
    }

    @Override
    @Transactional
    public HotelAdminResponse update(Long hotelId, Long id, HotelAdminUpdateRequest request) {
        AdminUser admin = findAdminById(id, hotelId);

        log.debug("호텔 관리자 수정 시작 - id: {}, userName: {} → {}, roleId: {} → {}",
                id, admin.getUserName(), request.getUserName(), admin.getRoleId(), request.getRoleId());

        admin.updateProfile(
                request.getUserName(), request.getEmail(), request.getPhone(),
                request.getMobileCountryCode(), request.getMobile(),
                request.getPhoneCountryCode(),
                request.getDepartment(), request.getPosition(),
                request.getRoleName(), request.getRoleId(), request.getUseYn());

        AdminUser saved = adminUserRepository.saveAndFlush(admin);
        log.debug("호텔 관리자 flush 완료 - id: {}, userName: {}", saved.getId(), saved.getUserName());

        // 프로퍼티 매핑 갱신 (삭제 후 재등록)
        adminUserPropertyRepository.deleteByAdminUserId(admin.getId());
        savePropertyMappings(admin.getId(), request.getPropertyIds());

        log.info("호텔 관리자 수정 완료: {} ({})", saved.getUserName(), saved.getLoginId());
        return getDetail(hotelId, saved.getId());
    }

    @Override
    @Transactional
    public void delete(Long hotelId, Long id) {
        AdminUser admin = findAdminById(id, hotelId);
        admin.softDelete();
        adminUserPropertyRepository.deleteByAdminUserId(admin.getId());
        log.info("호텔 관리자 삭제: {} ({})", admin.getUserName(), admin.getLoginId());
    }

    @Override
    public boolean checkLoginId(String loginId) {
        return adminUserRepository.existsByLoginIdAndDeletedAtIsNull(loginId);
    }

    @Override
    @Transactional
    public void resetPassword(Long hotelId, Long id) {
        AdminUser admin = findAdminById(id, hotelId);
        admin.resetPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        log.info("호텔 관리자 비밀번호 초기화: {} ({})", admin.getUserName(), admin.getLoginId());
    }

    // === private 메서드 ===

    private AdminUser findAdminById(Long id, Long hotelId) {
        return adminUserRepository.findById(id)
                .filter(a -> a.getDeletedAt() == null)
                .filter(a -> hotelId.equals(a.getHotelId()))
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private String generateMemberNumber() {
        Long nextVal = adminUserRepository.getNextMemberNumberSequence();
        return String.format("U%09d", nextVal);
    }

    private void savePropertyMappings(Long adminUserId, List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return;
        }
        List<AdminUserProperty> mappings = propertyIds.stream()
                .map(propertyId -> AdminUserProperty.builder()
                        .adminUserId(adminUserId)
                        .propertyId(propertyId)
                        .build())
                .collect(Collectors.toList());
        adminUserPropertyRepository.saveAllAndFlush(mappings);
        log.debug("프로퍼티 매핑 저장 완료 - adminUserId: {}, propertyIds: {}", adminUserId, propertyIds);
    }

    /**
     * 아이디 마스킹: 앞 5자 + *****
     * ex) admin01234 → admin*****
     */
    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.length() <= 5) return loginId;
        return loginId.substring(0, 5) + "*****";
    }

    /**
     * 이름 마스킹: 성 + * + 끝자
     * ex) 홍길동 → 홍*동, 김수 → 김*수
     */
    private String maskUserName(String userName) {
        if (userName == null || userName.length() < 2) return userName;
        if (userName.length() == 2) {
            return userName.charAt(0) + "*";
        }
        return userName.charAt(0) + "*" + userName.charAt(userName.length() - 1);
    }
}
