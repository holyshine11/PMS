package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 요금 상세 조회 응답
 */
@Getter
@Builder
public class PriceCheckResponse {

    private final Long roomTypeId;
    private final String roomTypeName;
    private final Long rateCodeId;
    private final String rateNameKo;
    private final String currency;
    private final List<AvailableRoomTypeResponse.DailyPrice> dailyCharges;
    private final BigDecimal totalSupply;
    private final BigDecimal totalTax;
    private final BigDecimal totalServiceCharge;
    private final BigDecimal grandTotal;
}
