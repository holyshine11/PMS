package com.hola.reservation.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.reservation.booking.dto.BookingResponse;
import com.hola.reservation.booking.entity.BookingApiKey;
import com.hola.reservation.booking.service.BookingApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

/**
 * 부킹엔진 API Key 인증 필터
 * - API-KEY + VENDOR-ID 헤더가 있으면 검증 (실패 시 401)
 * - 헤더가 없으면 통과 (게스트 UI 접근 허용)
 */
@Slf4j
@RequiredArgsConstructor
public class BookingApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_API_KEY = "API-KEY";
    private static final String HEADER_VENDOR_ID = "VENDOR-ID";

    private final BookingApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(HEADER_API_KEY);
        String vendorId = request.getHeader(HEADER_VENDOR_ID);

        // 헤더가 없으면 통과 (게스트 접근)
        if (apiKey == null && vendorId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 헤더가 하나만 있으면 에러
        if (apiKey == null || vendorId == null) {
            sendError(response, "API-KEY와 VENDOR-ID 헤더가 모두 필요합니다.");
            return;
        }

        // API Key 검증
        String clientIp = request.getRemoteAddr();
        Optional<BookingApiKey> validKey = apiKeyService.validateApiKey(vendorId, apiKey, clientIp);

        if (validKey.isEmpty()) {
            sendError(response, "유효하지 않은 API Key입니다.");
            return;
        }

        // 인증 정보 설정 (ROLE_BOOKING_VENDOR)
        BookingApiKey key = validKey.get();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        key.getVendorId(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_BOOKING_VENDOR")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        BookingResponse<Void> errorResponse = BookingResponse.error("HOLA-4090", message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
