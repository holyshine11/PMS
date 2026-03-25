package com.hola.reservation.booking.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * PG 인증 후 결제승인 요청 DTO
 */
@Getter
@Builder
public class ApproveAfterAuthRequest {

    /** 인증 거래번호 (결제창에서 수신) */
    private final String authorizationId;

    /** 상점 주문번호 */
    private final String orderId;

    /** 요청 금액 (응답 금액과 비교 검증용) */
    private final BigDecimal expectedAmount;
}
