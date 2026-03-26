package com.hola.hotel.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * 해석된 최종 청소 정책 (프로퍼티 기본값 + 룸타입 오버라이드 병합 결과)
 */
@Getter
@Builder
public class ResolvedCleaningPolicy {

    private final boolean stayoverEnabled;
    private final int stayoverFrequency;
    private final boolean turndownEnabled;
    private final BigDecimal stayoverCredit;
    private final BigDecimal turndownCredit;
    private final String stayoverPriority;
    private final String dndPolicy;
    private final int dndMaxSkipDays;
    private final boolean overridden;
}
