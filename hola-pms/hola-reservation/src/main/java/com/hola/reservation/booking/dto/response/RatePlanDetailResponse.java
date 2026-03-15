package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 패키지(레이트플랜) 상세 응답 (산하 2.5 대응)
 * - 요금 상세, 취소정책, 노쇼정책
 */
@Getter
@Builder
@AllArgsConstructor
public class RatePlanDetailResponse {

    /** 레이트플랜 ID */
    private final Long ratePlanId;

    /** 레이트 코드 */
    private final String rateCode;

    /** 패키지명 */
    private final String ratePlanName;

    /** 패키지명 (영문) */
    private final String ratePlanNameEn;

    /** 카테고리 */
    private final String category;

    /** 통화 */
    private final String currency;

    /** 판매 기간 */
    private final LocalDate saleStartDate;
    private final LocalDate saleEndDate;

    /** 숙박일수 제한 */
    private final Integer minStayDays;
    private final Integer maxStayDays;

    /** 취소 정책 목록 */
    private final List<CancellationPolicyInfo> cancellationPolicies;

    /** 노쇼 정책 */
    private final NoShowPolicyInfo noShowPolicy;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CancellationPolicyInfo {
        /** 기준 (BEFORE_CHECKIN, ON_CHECKIN 등) */
        private final String basis;

        /** 체크인 N일 전 */
        private final Integer daysBefore;

        /** 수수료 금액/비율 */
        private final BigDecimal feeAmount;

        /** 수수료 유형 (PERCENTAGE, FIXED, FIRST_NIGHT) */
        private final String feeType;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class NoShowPolicyInfo {
        /** 수수료 금액/비율 */
        private final BigDecimal feeAmount;

        /** 수수료 유형 */
        private final String feeType;
    }
}
