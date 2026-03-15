package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 부킹엔진 유료 서비스 조회 응답
 */
@Getter
@Builder
public class AddOnServiceResponse {

    /** 서비스 ID */
    private final Long serviceId;

    /** 서비스 코드 */
    private final String serviceCode;

    /** 서비스명 (한글) */
    private final String serviceNameKo;

    /** 서비스명 (영문) */
    private final String serviceNameEn;

    /** 서비스 유형 (ROOM_AMENITY, BREAKFAST, ROOM_SERVICE) */
    private final String serviceType;

    /** 적용 박수 (FIRST_NIGHT_ONLY, ALL_NIGHTS, NOT_APPLICABLE) */
    private final String applicableNights;

    /** 통화 */
    private final String currencyCode;

    /** VAT 포함 가격 */
    private final BigDecimal price;

    /** 공급가 */
    private final BigDecimal supplyPrice;

    /** 세금 */
    private final BigDecimal taxAmount;

    /** 수량 */
    private final Integer quantity;

    /** 수량 단위 (EA, SET, TIME, SERVICE) */
    private final String quantityUnit;
}
