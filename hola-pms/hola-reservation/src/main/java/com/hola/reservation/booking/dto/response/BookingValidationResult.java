package com.hola.reservation.booking.dto.response;

import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.ReservationChannel;
import com.hola.rate.entity.RateCode;
import com.hola.reservation.entity.DailyCharge;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 부킹 요청 검증 결과 DTO
 * - KICC 결제 플로우에서 검증 + 거래등록 후, 승인 시 예약 생성에 재사용
 */
@Getter
@Builder
public class BookingValidationResult {

    /** 프로퍼티 */
    private final Property property;

    /** WEBSITE 예약채널 (nullable) */
    private final ReservationChannel websiteChannel;

    /** 가장 이른 체크인 */
    private final LocalDate earliestCheckIn;

    /** 가장 늦은 체크아웃 */
    private final LocalDate latestCheckOut;

    /** 총 결제 금액 */
    private final BigDecimal grandTotal;

    /** 객실별 일별 요금 목록 */
    private final List<List<DailyCharge>> roomDailyChargesList;

    /** 객실별 레이트코드 */
    private final List<RateCode> roomRateCodes;
}
