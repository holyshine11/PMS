package com.hola.common.security;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 접근 권한 검증 공통 서비스
 * - Controller에서 Repository 직접 참조 제거
 * - SecurityContextHolder 보일러플레이트 캡슐화
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;

    /** 현재 로그인 사용자 조회 */
    public AdminUser getCurrentUser() {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminUserRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));
    }

    /** 현재 로그인 ID 조회 */
    public String getCurrentLoginId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * 호텔 접근 권한 검증
     * SUPER_ADMIN: 전체 허용, HOTEL_ADMIN/PROPERTY_ADMIN: 소속 호텔만
     */
    public void validateHotelAccess(Long hotelId) {
        AdminUser currentUser = getCurrentUser();
        if (currentUser.isSuperAdmin()) return;

        if (!hotelId.equals(currentUser.getHotelId())) {
            throw new HolaException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 프로퍼티 접근 권한 검증
     * SUPER_ADMIN: 전체 허용, HOTEL_ADMIN/PROPERTY_ADMIN: 매핑된 프로퍼티만
     */
    public void validatePropertyAccess(Long propertyId) {
        AdminUser currentUser = getCurrentUser();
        if (currentUser.isSuperAdmin()) return;

        boolean hasAccess = adminUserPropertyRepository
                .existsByAdminUserIdAndPropertyId(currentUser.getId(), propertyId);
        if (!hasAccess) {
            throw new HolaException(ErrorCode.FORBIDDEN);
        }
    }
}
