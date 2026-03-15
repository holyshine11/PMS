package com.hola.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode - 에러 코드 체계 검증")
class ErrorCodeTest {

    @Test
    @DisplayName("모든 에러 코드 고유성 검증 - 중복 코드 없음")
    void allErrorCodes_uniqueCodes() {
        List<String> codes = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .toList();

        Map<String, Long> duplicates = codes.stream()
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates)
                .as("중복된 에러 코드가 없어야 함: %s", duplicates)
                .isEmpty();
    }

    @Test
    @DisplayName("에러 코드 형식 - HOLA-XXXX 패턴")
    void allErrorCodes_validFormat() {
        Arrays.stream(ErrorCode.values()).forEach(errorCode ->
                assertThat(errorCode.getCode())
                        .as("%s의 코드 형식", errorCode.name())
                        .matches("HOLA-\\d{4}")
        );
    }

    @Test
    @DisplayName("HttpStatus 매핑 일관성 - NOT_FOUND 코드는 404")
    void errorCodes_httpStatusConsistency() {
        // NOT_FOUND 계열 코드는 404
        Arrays.stream(ErrorCode.values())
                .filter(e -> e.name().endsWith("NOT_FOUND"))
                .forEach(errorCode ->
                        assertThat(errorCode.getHttpStatus())
                                .as("%s는 404여야 함", errorCode.name())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                );

        // DUPLICATE 계열 코드는 409
        Arrays.stream(ErrorCode.values())
                .filter(e -> e.name().endsWith("DUPLICATE"))
                .forEach(errorCode ->
                        assertThat(errorCode.getHttpStatus())
                                .as("%s는 409여야 함", errorCode.name())
                                .isEqualTo(HttpStatus.CONFLICT)
                );
    }

    @Test
    @DisplayName("예약 에러 코드 대역 - HOLA-4xxx")
    void reservationErrors_correctCodeRange() {
        Arrays.stream(ErrorCode.values())
                .filter(e -> e.name().startsWith("RESERVATION_") ||
                             e.name().startsWith("SUB_RESERVATION_") ||
                             e.name().startsWith("BOOKING_") ||
                             e.name().startsWith("EARLY_LATE_") ||
                             e.name().startsWith("DEPOSIT_"))
                .forEach(errorCode ->
                        assertThat(errorCode.getCode())
                                .as("%s는 HOLA-4xxx 대역이어야 함", errorCode.name())
                                .startsWith("HOLA-4")
                );
    }
}
