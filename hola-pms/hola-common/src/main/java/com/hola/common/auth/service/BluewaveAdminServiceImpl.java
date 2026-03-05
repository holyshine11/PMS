package com.hola.common.auth.service;

import com.hola.common.auth.dto.BluewaveAdminCreateRequest;
import com.hola.common.auth.dto.BluewaveAdminListResponse;
import com.hola.common.auth.dto.BluewaveAdminResponse;
import com.hola.common.auth.dto.BluewaveAdminUpdateRequest;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BluewaveAdminServiceImpl implements BluewaveAdminService {

    private static final String DEFAULT_PASSWORD = "holapms1!";
    private static final String ACCOUNT_TYPE = "BLUEWAVE_ADMIN";
    private static final String ROLE = "SUPER_ADMIN";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<BluewaveAdminListResponse> getList(String loginId, String userName, Boolean useYn) {
        // DB 조회 후 Java 필터링 (JPQL null 조건부 LIKE → PostgreSQL bytea 타입 캐스팅 이슈 방지)
        List<AdminUser> admins = adminUserRepository
                .findByAccountTypeAndDeletedAtIsNullOrderByCreatedAtDesc(ACCOUNT_TYPE)
                .stream()
                .filter(a -> loginId == null || loginId.isEmpty() || a.getLoginId().contains(loginId))
                .filter(a -> userName == null || userName.isEmpty() || a.getUserName().contains(userName))
                .filter(a -> useYn == null || a.getUseYn().equals(useYn))
                .collect(Collectors.toList());

        return admins.stream().map(admin -> BluewaveAdminListResponse.builder()
                .id(admin.getId())
                .loginId(maskLoginId(admin.getLoginId()))
                .userName(maskUserName(admin.getUserName()))
                .accountType("블루웨이브 관리자")
                .useYn(admin.getUseYn())
                .createdAt(admin.getCreatedAt() != null ? admin.getCreatedAt().format(DATE_FORMAT) : "")
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public BluewaveAdminResponse getDetail(Long id) {
        AdminUser admin = findAdminById(id);

        return BluewaveAdminResponse.builder()
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
                .accountType("블루웨이브 관리자")
                .useYn(admin.getUseYn())
                .createdAt(admin.getCreatedAt() != null ? admin.getCreatedAt().format(DATE_FORMAT) : "")
                .build();
    }

    @Override
    @Transactional
    public BluewaveAdminResponse create(BluewaveAdminCreateRequest request) {
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
                .role(ROLE)
                .memberNumber(memberNumber)
                .accountType(ACCOUNT_TYPE)
                .hotelId(null)
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            admin.deactivate();
        }

        AdminUser saved = adminUserRepository.save(admin);

        log.info("블루웨이브 관리자 생성: {} ({})", saved.getUserName(), saved.getLoginId());
        return getDetail(saved.getId());
    }

    @Override
    @Transactional
    public BluewaveAdminResponse update(Long id, BluewaveAdminUpdateRequest request) {
        AdminUser admin = findAdminById(id);

        log.debug("블루웨이브 관리자 수정 시작 - id: {}, userName: {} → {}",
                id, admin.getUserName(), request.getUserName());

        admin.updateProfile(
                request.getUserName(), request.getEmail(), request.getPhone(),
                request.getMobileCountryCode(), request.getMobile(),
                request.getPhoneCountryCode(),
                request.getDepartment(), request.getPosition(),
                request.getRoleName(), null, request.getUseYn());

        AdminUser saved = adminUserRepository.saveAndFlush(admin);
        log.debug("블루웨이브 관리자 flush 완료 - id: {}, userName: {}", saved.getId(), saved.getUserName());

        log.info("블루웨이브 관리자 수정 완료: {} ({})", saved.getUserName(), saved.getLoginId());
        return getDetail(saved.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdminUser admin = findAdminById(id);
        admin.softDelete();
        log.info("블루웨이브 관리자 삭제: {} ({})", admin.getUserName(), admin.getLoginId());
    }

    @Override
    public boolean checkLoginId(String loginId) {
        return adminUserRepository.existsByLoginIdAndDeletedAtIsNull(loginId);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        AdminUser admin = findAdminById(id);
        admin.resetPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        log.info("블루웨이브 관리자 비밀번호 초기화: {} ({})", admin.getUserName(), admin.getLoginId());
    }

    // === private 메서드 ===

    private AdminUser findAdminById(Long id) {
        return adminUserRepository.findById(id)
                .filter(a -> a.getDeletedAt() == null)
                .filter(a -> ACCOUNT_TYPE.equals(a.getAccountType()))
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));
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
