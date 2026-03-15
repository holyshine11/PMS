package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 내 예약 조회 응답 (이메일+성명 기반 예약 검색)
 */
@Getter
@Builder
public class BookingLookupResponse {

    /** 예약 확인번호 */
    private final String confirmationNo;

    /** 예약 상태 */
    private final String reservationStatus;

    /** 투숙객 이름 (한글) */
    private final String guestNameKo;

    /** 투숙객 이름 (영문) */
    private final String guestNameEn;

    /** 프로퍼티명 */
    private final String propertyName;

    /** 프로퍼티 코드 */
    private final String propertyCode;

    /** 체크인 날짜 */
    private final LocalDate checkIn;

    /** 체크아웃 날짜 */
    private final LocalDate checkOut;

    /** 객실 수 */
    private final int roomCount;

    /** 총 결제금액 */
    private final BigDecimal totalAmount;

    /** 통화 */
    private final String currency;

    /** 예약 생성일시 */
    private final LocalDateTime createdAt;
}
