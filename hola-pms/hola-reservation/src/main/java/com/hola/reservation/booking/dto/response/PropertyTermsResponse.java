package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 이용약관 응답
 */
@Getter
@Builder
public class PropertyTermsResponse {

    /** 약관 ID */
    private final Long termsId;

    /** 약관 유형 (BOOKING, PRIVACY, CANCELLATION, HOUSE_RULES) */
    private final String termsType;

    /** 제목 (한글) */
    private final String titleKo;

    /** 제목 (영문) */
    private final String titleEn;

    /** 내용 (한글) */
    private final String contentKo;

    /** 내용 (영문) */
    private final String contentEn;

    /** 버전 */
    private final String version;

    /** 필수 동의 여부 */
    private final boolean required;
}
