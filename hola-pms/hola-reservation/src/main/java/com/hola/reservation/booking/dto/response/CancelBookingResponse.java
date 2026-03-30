package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 게스트 자가 취소 결과 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelBookingResponse {

    /** 확인번호 */
    private String confirmationNo;

    /** 예약 상태 */
    private String status;

    /** 취소 수수료 금액 */
    private BigDecimal cancelFeeAmount;

    /** 환불 금액 */
    private BigDecimal refundAmount;

    /** PG 환불 성공 여부 (null = PG 결제 아님) */
    private Boolean pgRefundSuccess;
    /** PG 환불 승인번호 */
    private String pgRefundApprovalNo;
}
