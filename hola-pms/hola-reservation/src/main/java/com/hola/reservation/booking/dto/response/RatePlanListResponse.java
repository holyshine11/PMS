package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 패키지(레이트플랜) 목록 응답 (산하 2.4 대응)
 */
@Getter
@Builder
@AllArgsConstructor
public class RatePlanListResponse {

    /** 레이트코드 ID */
    private final Long ratePlanId;

    /** 레이트 코드 */
    private final String rateCode;

    /** 패키지명 (한글) */
    private final String ratePlanName;

    /** 패키지명 (영문) */
    private final String ratePlanNameEn;

    /** 카테고리 (BAR, PACKAGE, PROMOTION 등) */
    private final String category;

    /** 통화 */
    private final String currency;

    /** 최저가 (해당 기간 기준) */
    private final Long minPrice;

    /** 최소 숙박일 */
    private final Integer minStayDays;

    /** 최대 숙박일 */
    private final Integer maxStayDays;

    /** 적용 가능 객실타입 수 */
    private final int roomTypeCount;

    /** 적용 가능 객실타입 목록 */
    private final List<RoomTypeInfo> roomTypes;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RoomTypeInfo {
        private final Long roomTypeId;
        private final String roomTypeCode;
        private final String roomClassName;
        private final Long pricePerNight;
    }
}
