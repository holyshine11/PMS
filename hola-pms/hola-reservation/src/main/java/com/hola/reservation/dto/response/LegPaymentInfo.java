package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Leg별 결제 현황 정보 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegPaymentInfo {

    /** SubReservation ID */
    private Long subReservationId;

    /** 서브예약 번호 (e.g. "RSV-00001-01") */
    private String subReservationNo;

    /** 객실 타입명 */
    private String roomTypeName;

    /** Leg 상태 (RESERVED, INHOUSE, CHECKED_OUT, CANCELED 등) */
    private String roomReservationStatus;

    /** Leg 요금 합계 (DailyCharge + services + serviceCharge) */
    private BigDecimal legTotal;

    /** Leg에 귀속된 결제 합계 */
    private BigDecimal legPaid;

    /** Leg에 귀속된 환불 합계 */
    private BigDecimal legRefunded;

    /** Leg 잔액 = legTotal - (legPaid - legRefunded) */
    private BigDecimal legRemaining;
}
