package com.hola.reservation.booking.pg.kicc.dto;

import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import lombok.*;

import java.math.BigDecimal;

/**
 * KICC 결제 대기 중 Redis 임시 저장 데이터
 * - 거래등록 후 ~ 결제 승인 전까지 예약 데이터를 보관
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KiccBookingTempData {

    /** 프로퍼티 코드 */
    private String propertyCode;

    /** 부킹 요청 원본 */
    private BookingCreateRequest bookingRequest;

    /** 검증된 총 결제 금액 */
    private BigDecimal grandTotal;

    /** KICC 상점 주문번호 */
    private String shopOrderNo;

    /** 클라이언트 IP */
    private String clientIp;

    /** User-Agent */
    private String userAgent;
}
