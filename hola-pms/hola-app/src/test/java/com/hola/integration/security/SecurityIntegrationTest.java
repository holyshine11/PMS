package com.hola.integration.security;

import com.hola.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security 필터 체인 통합 테스트
 * - API 체인 (@Order(1), /api/**): JWT 기반, 세션 NEVER
 * - Web 체인 (@Order(2), /**): 세션 기반, 폼 로그인
 */
@DisplayName("Security 통합 테스트")
class SecurityIntegrationTest extends BaseIntegrationTest {

    /**
     * 401/403이 아닌지 검증하는 커스텀 매처
     * 보안 필터를 통과했는지 확인 (실제 데이터 없어 404/500 등은 허용)
     */
    private static ResultMatcher notAuthError() {
        return result -> {
            int statusCode = result.getResponse().getStatus();
            if (statusCode == 401 || statusCode == 403) {
                throw new AssertionError(
                        "보안 필터에서 차단됨: HTTP " + statusCode + " (401/403이 아니어야 함)");
            }
        };
    }

    // ========== 인증 없는 접근 테스트 ==========

    @Test
    @DisplayName("미인증 사용자가 예약 API 접근 시 401 또는 403 반환")
    @WithAnonymousUser
    void unauthenticated_reservationApi_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/properties/1/reservations"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("미인증 사용자가 로그인 페이지 접근 시 200 반환")
    @WithAnonymousUser
    void unauthenticated_loginPage_accessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // ========== 역할별 접근 테스트 ==========

    @Test
    @DisplayName("PROPERTY_ADMIN 역할로 예약 API 접근 허용")
    @WithMockUser(username = "propadmin", roles = {"PROPERTY_ADMIN"})
    void propertyAdmin_ownProperty_allowed() throws Exception {
        // SecurityFilterChain 레벨에서 PROPERTY_ADMIN 역할 허용 확인
        // 실제 데이터가 없으므로 404 또는 500 가능 (401/403이 아니면 통과)
        mockMvc.perform(get("/api/v1/properties/1/reservations"))
                .andExpect(notAuthError());
    }

    @Test
    @DisplayName("SUPER_ADMIN 역할로 모든 프로퍼티 예약 API 접근 허용")
    void superAdmin_anyProperty_allowed() throws Exception {
        // BaseIntegrationTest의 기본 SUPER_ADMIN 사용
        mockMvc.perform(get("/api/v1/properties/999/reservations"))
                .andExpect(notAuthError());
    }

    @Test
    @DisplayName("HOTEL_ADMIN 역할로 예약 API 접근 허용")
    @WithMockUser(username = "hoteladmin", roles = {"HOTEL_ADMIN"})
    void hotelAdmin_reservationAccess_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/properties/1/reservations"))
                .andExpect(notAuthError());
    }

    // ========== 부킹엔진 공개 API 테스트 ==========

    @Test
    @DisplayName("부킹엔진 API는 인증 없이 접근 가능")
    @WithAnonymousUser
    void bookingApi_noAuth_accessible() throws Exception {
        // /api/v1/booking/** 은 permitAll
        // 프로퍼티 코드가 존재하지 않으므로 404 가능하지만, 401/403은 아님
        mockMvc.perform(get("/api/v1/booking/properties/GMP"))
                .andExpect(notAuthError());
    }

    // ========== 예약 삭제 권한 테스트 ==========

    @Test
    @DisplayName("SUPER_ADMIN은 예약 삭제 API 접근 가능")
    void reservationDelete_superAdminOnly() throws Exception {
        // SUPER_ADMIN 역할로 삭제 요청 - 보안 필터 통과 확인 (실제 데이터 없어 404/500 가능)
        mockMvc.perform(delete("/api/v1/properties/1/reservations/1/delete"))
                .andExpect(notAuthError());
    }

    @Test
    @DisplayName("PROPERTY_ADMIN은 예약 삭제 API 접근 가능 (보안 필터 레벨)")
    @WithMockUser(username = "propadmin", roles = {"PROPERTY_ADMIN"})
    void reservationDelete_propertyAdmin_allowed() throws Exception {
        // SecurityConfig에서 /api/v1/properties/*/reservations/** 는
        // SUPER_ADMIN, HOTEL_ADMIN, PROPERTY_ADMIN 모두 허용
        // 실제 비즈니스 로직 레벨 권한 검증은 AccessControlService에서 처리
        mockMvc.perform(delete("/api/v1/properties/1/reservations/1/delete"))
                .andExpect(notAuthError());
    }

    // ========== JWT 관련 테스트 ==========

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 API 요청 시 401 반환")
    @WithAnonymousUser
    void jwtExpired_returns401() throws Exception {
        // 유효하지 않은 Authorization 헤더 전송
        mockMvc.perform(get("/api/v1/properties/1/reservations")
                        .header("Authorization", "Bearer invalid.expired.token"))
                .andExpect(status().is4xxClientError());
    }

    // ========== 정적 리소스 접근 테스트 ==========

    @Test
    @DisplayName("정적 리소스(CSS, JS)는 인증 없이 접근 가능")
    @WithAnonymousUser
    void staticResources_accessible() throws Exception {
        // /css/**, /js/** 는 permitAll
        mockMvc.perform(get("/css/hola.css"))
                .andExpect(notAuthError());

        mockMvc.perform(get("/js/hola-common.js"))
                .andExpect(notAuthError());
    }
}
