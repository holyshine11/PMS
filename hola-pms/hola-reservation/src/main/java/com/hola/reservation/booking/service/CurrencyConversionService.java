package com.hola.reservation.booking.service;

import com.hola.reservation.booking.entity.ExchangeRate;
import com.hola.reservation.booking.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/**
 * 다중 통화 변환 서비스
 * - 기준통화: KRW
 * - 지원통화: KRW, USD, JPY, CNY
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("KRW", "USD", "JPY", "CNY");
    private static final String BASE_CURRENCY = "KRW";

    private final ExchangeRateRepository exchangeRateRepository;

    /**
     * 지원 통화 여부 확인
     */
    public boolean isSupported(String currency) {
        return currency != null && SUPPORTED_CURRENCIES.contains(currency.toUpperCase());
    }

    /**
     * KRW 금액을 대상 통화로 변환
     * @param amountKrw KRW 금액
     * @param targetCurrency 대상 통화 코드
     * @return 변환된 금액 (KRW이면 그대로 반환)
     */
    public BigDecimal convert(BigDecimal amountKrw, String targetCurrency) {
        if (amountKrw == null) return BigDecimal.ZERO;
        if (targetCurrency == null || BASE_CURRENCY.equals(targetCurrency.toUpperCase())) {
            return amountKrw;
        }

        ExchangeRate rate = getLatestRate(targetCurrency);
        if (rate == null) {
            log.warn("[Currency] 환율 정보 없음: currency={}, KRW 금액 그대로 반환", targetCurrency);
            return amountKrw;
        }

        BigDecimal converted = amountKrw.multiply(rate.getRateValue());

        // 통화별 소수점 처리
        int scale = "JPY".equals(targetCurrency.toUpperCase()) ? 0 : 2;
        return converted.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * long 금액 변환 (편의 메서드)
     */
    public BigDecimal convert(long amountKrw, String targetCurrency) {
        return convert(BigDecimal.valueOf(amountKrw), targetCurrency);
    }

    /**
     * 최신 환율 조회
     */
    private ExchangeRate getLatestRate(String currency) {
        Optional<ExchangeRate> rate = exchangeRateRepository.findLatestRate(
                currency.toUpperCase(), LocalDate.now());
        return rate.orElse(null);
    }
}
