package com.hola.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 모바일 하우스키핑 전용 세션 인증 필터.
 * PMS Admin의 SecurityContext(SPRING_SECURITY_CONTEXT)를 오염시키지 않도록
 * 요청 전 원본 컨텍스트를 백업하고, 요청 후 복원한다.
 *
 * 같은 브라우저에서 PMS(SUPER_ADMIN) + 모바일(HOUSEKEEPER) 동시 사용 시
 * 세션의 SPRING_SECURITY_CONTEXT가 덮어씌워지는 것을 방지.
 */
public class HkMobileSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HkMobileSessionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 원본 SecurityContext 백업 (PMS Admin 컨텍스트 보호)
        SecurityContext originalContext = SecurityContextHolder.getContext();

        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Long userId = (Long) session.getAttribute("hkUserId");
                String role = (String) session.getAttribute("hkUserRole");
                if (userId != null && role != null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            userId, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    // 모바일 전용 SecurityContext (이 요청에서만 사용)
                    SecurityContext mobileContext = SecurityContextHolder.createEmptyContext();
                    mobileContext.setAuthentication(auth);
                    SecurityContextHolder.setContext(mobileContext);
                }
            }
            chain.doFilter(request, response);
        } finally {
            // 요청 완료 후 원본 컨텍스트 복원 → PMS 세션 오염 방지
            SecurityContextHolder.setContext(originalContext);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.contains("/hk-mobile/");
    }
}
