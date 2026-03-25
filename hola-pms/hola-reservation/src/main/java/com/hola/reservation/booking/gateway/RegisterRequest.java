package com.hola.reservation.booking.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * PG 거래등록 요청 DTO
 */
@Getter
@Builder
public class RegisterRequest {

    /** 상점 주문번호 (예약번호 기반) */
    private final String orderId;

    /** 결제 금액 */
    private final BigDecimal amount;

    /** 상품명 (예: "디럭스 더블룸 1박") */
    private final String goodsName;

    /** 고객명 */
    private final String customerName;

    /** 고객 연락처 */
    private final String customerPhone;

    /** 고객 이메일 */
    private final String customerEmail;

    /** 결제수단 ("CARD", "BILLING" 등) */
    private final String paymentMethod;

    /** 디바이스 타입 ("pc" / "mobile") */
    @Builder.Default
    private final String deviceType = "pc";
}
