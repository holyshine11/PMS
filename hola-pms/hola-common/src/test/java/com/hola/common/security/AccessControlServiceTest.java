package com.hola.common.security;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * 접근 권한 검증 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccessControlService")
class AccessControlServiceTest {

    @InjectMocks
    private AccessControlService accessControlService;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AdminUserPropertyRepository adminUserPropertyRepository;

    private void setupSecurityContext(String loginId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginId, null));
    }

    private AdminUser createAdminUser(String role, Long hotelId) {
        return AdminUser.builder()
                .loginId("testuser")
                .password("encoded")
                .userName("테스트")
                .role(role)
                .hotelId(hotelId)
                .build();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 모든 호텔에 접근 가능")
    void validateHotelAccess_superAdmin_allowed() {
        setupSecurityContext("admin");
        AdminUser superAdmin = createAdminUser("SUPER_ADMIN", null);
        when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("admin"))
                .thenReturn(Optional.of(superAdmin));

        assertDoesNotThrow(() -> accessControlService.validateHotelAccess(999L));
    }

    @Test
    @DisplayName("HOTEL_ADMIN은 자기 호텔만 접근 가능")
    void validateHotelAccess_hotelAdmin_ownHotel() {
        setupSecurityContext("hoteladmin");
        AdminUser hotelAdmin = createAdminUser("HOTEL_ADMIN", 1L);
        when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("hoteladmin"))
                .thenReturn(Optional.of(hotelAdmin));

        assertDoesNotThrow(() -> accessControlService.validateHotelAccess(1L));
    }

    @Test
    @DisplayName("HOTEL_ADMIN은 타 호텔 접근 시 FORBIDDEN")
    void validateHotelAccess_hotelAdmin_otherHotel_forbidden() {
        setupSecurityContext("hoteladmin");
        AdminUser hotelAdmin = createAdminUser("HOTEL_ADMIN", 1L);
        when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("hoteladmin"))
                .thenReturn(Optional.of(hotelAdmin));

        assertThatThrownBy(() -> accessControlService.validateHotelAccess(2L))
                .isInstanceOf(HolaException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("SUPER_ADMIN은 모든 프로퍼티에 접근 가능")
    void validatePropertyAccess_superAdmin_allowed() {
        setupSecurityContext("admin");
        AdminUser superAdmin = createAdminUser("SUPER_ADMIN", null);
        when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("admin"))
                .thenReturn(Optional.of(superAdmin));

        assertDoesNotThrow(() -> accessControlService.validatePropertyAccess(999L));
    }

    @Test
    @DisplayName("PROPERTY_ADMIN은 매핑된 프로퍼티만 접근 가능")
    void validatePropertyAccess_propertyAdmin_mapped() {
        setupSecurityContext("propadmin");
        AdminUser propAdmin = createAdminUser("PROPERTY_ADMIN", 1L);
        when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("propadmin"))
                .thenReturn(Optional.of(propAdmin));
        when(adminUserPropertyRepository.existsByAdminUserIdAndPropertyId(propAdmin.getId(), 10L))
                .thenReturn(true);

        assertDoesNotThrow(() -> accessControlService.validatePropertyAccess(10L));
    }

    @Test
    @DisplayName("PROPERTY_ADMIN은 미매핑 프로퍼티 접근 시 FORBIDDEN")
    void validatePropertyAccess_propertyAdmin_notMapped_forbidden() {
        setupSecurityContext("propadmin");
        AdminUser propAdmin = createAdminUser("PROPERTY_ADMIN", 1L);
        when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("propadmin"))
                .thenReturn(Optional.of(propAdmin));
        when(adminUserPropertyRepository.existsByAdminUserIdAndPropertyId(propAdmin.getId(), 99L))
                .thenReturn(false);

        assertThatThrownBy(() -> accessControlService.validatePropertyAccess(99L))
                .isInstanceOf(HolaException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
