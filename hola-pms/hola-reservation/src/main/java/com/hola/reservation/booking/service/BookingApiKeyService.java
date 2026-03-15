package com.hola.reservation.booking.service;

import com.hola.reservation.booking.entity.BookingApiKey;
import com.hola.reservation.booking.repository.BookingApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 부킹엔진 API Key 검증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingApiKeyService {

    private final BookingApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * API Key 검증
     * @param vendorId VENDOR-ID 헤더 값
     * @param rawApiKey API-KEY 헤더 값 (평문)
     * @param clientIp 클라이언트 IP
     * @return 유효한 BookingApiKey (검증 실패 시 empty)
     */
    @Transactional
    public Optional<BookingApiKey> validateApiKey(String vendorId, String rawApiKey, String clientIp) {
        Optional<BookingApiKey> optKey = apiKeyRepository.findByVendorIdAndActiveTrue(vendorId);

        if (optKey.isEmpty()) {
            log.warn("[Booking Auth] 벤더 미등록: vendorId={}", vendorId);
            return Optional.empty();
        }

        BookingApiKey apiKey = optKey.get();

        // 유효성 확인 (활성 + 만료일)
        if (!apiKey.isValid()) {
            log.warn("[Booking Auth] 만료/비활성 키: vendorId={}", vendorId);
            return Optional.empty();
        }

        // IP 허용 확인
        if (!apiKey.isIpAllowed(clientIp)) {
            log.warn("[Booking Auth] IP 차단: vendorId={}, ip={}", vendorId, clientIp);
            return Optional.empty();
        }

        // BCrypt 키 비교
        if (!passwordEncoder.matches(rawApiKey, apiKey.getApiKeyHash())) {
            log.warn("[Booking Auth] 키 불일치: vendorId={}", vendorId);
            return Optional.empty();
        }

        // 마지막 사용 시간 갱신
        apiKey.updateLastUsedAt();

        log.debug("[Booking Auth] 인증 성공: vendorId={}", vendorId);
        return Optional.of(apiKey);
    }
}
