package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가용 객실타입 응답 (요금 포함)
 */
@Getter
@Builder
public class AvailableRoomTypeResponse {

    private final Long roomTypeId;
    private final String roomTypeCode;
    private final String roomClassName;
    private final String description;
    private final BigDecimal roomSize;
    private final String features;
    private final int maxAdults;
    private final int maxChildren;
    private final int availableCount;

    /** 적용 가능 레이트 옵션 목록 */
    private final List<RateOption> rateOptions;

    /** 무료 서비스 목록 */
    private final List<ServiceInfo> freeServices;

    @Getter
    @Builder
    public static class RateOption {
        private final Long rateCodeId;
        private final String rateCode;
        private final String rateNameKo;
        private final String currency;
        private final BigDecimal totalAmount;
        private final List<DailyPrice> dailyPrices;
        /** 레이트코드에 포함된 유료 서비스 */
        private final List<IncludedServiceInfo> includedServices;
        /** 숙박유형: OVERNIGHT 또는 DAY_USE */
        private final String stayType;
        /** Dayuse 이용시간 (시간 단위, DAY_USE일 때만) */
        private final Integer dayUseDurationHours;
    }

    @Getter
    @Builder
    public static class IncludedServiceInfo {
        private final Long serviceOptionId;
        private final String nameKo;
        private final String type;
        private final String applicableNights;
    }

    @Getter
    @Builder
    public static class DailyPrice {
        private final String date;
        private final BigDecimal supplyPrice;
        private final BigDecimal tax;
        private final BigDecimal serviceCharge;
        private final BigDecimal total;
    }

    @Getter
    @Builder
    public static class ServiceInfo {
        private final String nameKo;
        private final String type;
        private final int quantity;
    }
}
