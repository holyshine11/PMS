package com.hola.reservation.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.reservation.booking.service.BookingApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 부킹엔진 API 보안 설정
 * - @Order(0): 기존 API 체인(@Order(1))보다 먼저 매칭
 * - /api/v1/booking/** 경로에만 적용
 * - API-KEY 인증 필터 추가 (선택적 인증)
 */
@Configuration
@RequiredArgsConstructor
public class BookingSecurityConfig {

    private final BookingApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(0)
    public SecurityFilterChain bookingApiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/booking/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .addFilterBefore(bookingRateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(bookingApiKeyFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BookingRateLimitFilter bookingRateLimitFilter() {
        // IP당 60초 윈도우에 최대 60건 (확인번호 brute-force 방지)
        return new BookingRateLimitFilter(60, 60);
    }

    @Bean
    public BookingApiKeyFilter bookingApiKeyFilter() {
        return new BookingApiKeyFilter(apiKeyService, objectMapper);
    }
}
