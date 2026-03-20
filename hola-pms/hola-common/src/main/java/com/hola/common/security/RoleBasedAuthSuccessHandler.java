package com.hola.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * 로그인 성공 시 역할별 리다이렉트 처리
 * - HOUSEKEEPER / HOUSEKEEPING_SUPERVISOR → 모바일 로그인 안내
 * - 그 외 (SUPER_ADMIN, HOTEL_ADMIN, PROPERTY_ADMIN) → Admin 대시보드
 */
@Component
public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Set<String> MOBILE_ONLY_ROLES = Set.of(
            "ROLE_HOUSEKEEPER", "ROLE_HOUSEKEEPING_SUPERVISOR"
    );

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        if (MOBILE_ONLY_ROLES.contains(role)) {
            // 하우스키퍼 역할은 PMS 대시보드 접근 불가 → 모바일 안내
            request.getSession().invalidate();
            response.sendRedirect("/login?hk=true");
        } else {
            response.sendRedirect("/admin/dashboard");
        }
    }
}
