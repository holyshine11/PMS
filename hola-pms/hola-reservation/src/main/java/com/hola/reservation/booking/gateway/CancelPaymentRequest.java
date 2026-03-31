package com.hola.reservation.booking.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * PG 결제 취소 요청 DTO
 */
@Getter
@Builder
public class CancelPaymentRequest {

    /** PG 거래고유번호 */
    private final String pgCno;

    /** 취소 유형 ("FULL" / "PARTIAL") */
    private final String cancelType;

    /** 취소 금액 (부분취소 시) */
    private final BigDecimal cancelAmount;

    /** 취소 가능 잔액 — KICC: 원결제금액 (부분취소 시 검증용) */
    private final BigDecimal remainAmount;

    /** 취소 사유 */
    private final String reason;

    public boolean isPartialCancel() {
        return "PARTIAL".equals(cancelType);
    }
}
