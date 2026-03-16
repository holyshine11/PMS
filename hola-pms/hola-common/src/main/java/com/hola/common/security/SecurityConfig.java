package com.hola.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * API 보안 (JWT, Stateless)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            // NEVER: 세션 생성 안 함, 기존 세션은 허용 (웹 UI AJAX 호출 지원)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER))
            .authorizeHttpRequests(auth -> auth
                // Swagger UI / OpenAPI 문서
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                // 부킹엔진 API: BookingSecurityConfig(@Order(0))에서 별도 처리
                // 내 프로필 API: 모든 인증 사용자
                .requestMatchers("/api/v1/my-profile/**").authenticated()
                // selector API: 모든 인증 사용자 허용 (헤더 드롭다운)
                .requestMatchers("/api/v1/hotels/selector").authenticated()
                .requestMatchers("/api/v1/properties/selector").authenticated()
                // 블루웨이브 관리자 API: SUPER_ADMIN 전용
                .requestMatchers("/api/v1/bluewave-admins/**").hasRole("SUPER_ADMIN")
                // 호텔 관리자 API: SUPER_ADMIN + HOTEL_ADMIN 전용
                .requestMatchers("/api/v1/hotels/*/admins/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN")
                // 호텔 하위 프로퍼티 조회: 관리자 폼 드롭다운용 (모든 인증 사용자)
                .requestMatchers("/api/v1/hotels/*/properties").authenticated()
                // 프로퍼티 관리자 API: 모든 인증 사용자 허용
                .requestMatchers("/api/v1/properties/*/admins/**").authenticated()
                // 객실관리 API: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/api/v1/properties/*/room-classes/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/api/v1/properties/*/room-types/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/api/v1/properties/*/free-service-options/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/api/v1/properties/*/paid-service-options/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 레이트관리 API: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/api/v1/properties/*/rate-codes/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 프로모션코드 API: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/api/v1/properties/*/promotion-codes/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 얼리/레이트 정책 API: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/api/v1/properties/*/early-late-policies/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 예약채널 API: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/api/v1/properties/*/reservation-channels/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 예약관리 API: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/api/v1/properties/*/reservations/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 층/호수 조회: 객실 타입 폼에서 사용
                .requestMatchers("/api/v1/properties/*/floors/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/api/v1/properties/*/room-numbers/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 트랜잭션 코드 API: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/api/v1/properties/*/transaction-code-groups/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/api/v1/properties/*/transaction-codes/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 재고 관리 API
                .requestMatchers("/api/v1/properties/*/inventory-items/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 호텔관리 API: SUPER_ADMIN 전용 (제너릭 경로는 마지막)
                .requestMatchers("/api/v1/hotels/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/properties/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/market-codes/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/floors/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/room-numbers/**").hasRole("SUPER_ADMIN")
                // 권한관리 API: 역할 selector는 모든 인증 사용자 허용 (관리자 폼 드롭다운)
                .requestMatchers("/api/v1/hotel-admin-roles/selector").authenticated()
                .requestMatchers("/api/v1/property-admin-roles/selector").authenticated()
                // 권한관리 API: 호텔 권한 = SUPER_ADMIN 전용, 프로퍼티 권한 = SUPER_ADMIN + HOTEL_ADMIN
                .requestMatchers("/api/v1/hotel-admin-roles/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/property-admin-roles/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"code\":\"HOLA-0005\",\"message\":\"인증이 필요합니다.\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"code\":\"HOLA-0006\",\"message\":\"접근 권한이 없습니다.\"}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 웹 페이지 보안 (세션 기반, Thymeleaf)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/uploads/**",
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // 부킹엔진 게스트 화면: 인증 없이 접근 허용
                .requestMatchers("/booking/**").permitAll()
                // 내 프로필: 모든 인증 사용자
                .requestMatchers("/admin/my-profile/**").authenticated()
                // 호텔관리 전체: SUPER_ADMIN 전용
                .requestMatchers("/admin/hotels/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/properties/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/market-codes/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/floors/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/room-numbers/**").hasRole("SUPER_ADMIN")
                // 객실관리 웹: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/admin/room-classes/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/admin/room-types/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/admin/free-service-options/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                .requestMatchers("/admin/paid-service-options/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 레이트관리 웹: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/admin/rate-codes/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 프로모션코드 웹: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/admin/promotion-codes/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 얼리/레이트 정책 웹: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/admin/early-late-policies/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 예약채널 웹: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/admin/reservation-channels/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 예약관리 웹: SUPER_ADMIN + HOTEL_ADMIN + PROPERTY_ADMIN
                .requestMatchers("/admin/reservations/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN", "PROPERTY_ADMIN")
                // 회원관리
                .requestMatchers("/admin/members/bluewave-admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/members/hotel-admins/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN")
                // 권한관리: 호텔 권한 = SUPER_ADMIN 전용, 프로퍼티 권한 = SUPER_ADMIN + HOTEL_ADMIN
                .requestMatchers("/admin/roles/hotel-admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/roles/property-admins/**").hasAnyRole("SUPER_ADMIN", "HOTEL_ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
