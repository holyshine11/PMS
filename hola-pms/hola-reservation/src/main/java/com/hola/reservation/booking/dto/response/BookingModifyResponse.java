package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 예약 수정 응답
 */
@Getter
@Builder
public class BookingModifyResponse {

    /** 확인번호 */
    private final String confirmationNo;

    /** 수정 결과 상태 */
    private final String status;

    /** 변경된 체크인 */
    private final LocalDate checkIn;

    /** 변경된 체크아웃 */
    private final LocalDate checkOut;

    /** 변경된 성인 수 */
    private final int adults;

    /** 변경된 아동 수 */
    private final int children;

    /** 변경 전 총액 */
    private final BigDecimal previousAmount;

    /** 변경 후 총액 */
    private final BigDecimal newAmount;

    /** 차액 (양수: 추가결제, 음수: 환불) */
    private final BigDecimal priceDifference;

    /** 안내 메시지 */
    private final String message;
}
