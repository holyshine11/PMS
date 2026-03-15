package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프로모션 코드 검증 응답
 */
@Getter
@Builder
public class PromotionValidationResponse {

    /** 프로모션 코드 */
    private final String promotionCode;

    /** 유효 여부 */
    private final boolean valid;

    /** 프로모션 타입 (PROMOTION, EARLY_BIRD 등) */
    private final String promotionType;

    /** 한글 설명 */
    private final String descriptionKo;

    /** 영문 설명 */
    private final String descriptionEn;

    /** 할인/할증 방향 (D: 할인, U: 할증) */
    private final String downUpSign;

    /** 할인/할증 값 */
    private final BigDecimal downUpValue;

    /** 할인/할증 단위 (PERCENT, KRW, USD 등) */
    private final String downUpUnit;

    /** 프로모션 시작일 */
    private final LocalDate startDate;

    /** 프로모션 종료일 */
    private final LocalDate endDate;

    /** 검증 실패 시 사유 */
    private final String invalidReason;
}
