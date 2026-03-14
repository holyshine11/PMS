package com.hola.reservation.booking.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 결제 게이트웨이 요청 DTO
 */
@Getter
@Builder
public class PaymentRequest {

    private final String orderId;           // 주문 식별자 (예약번호)
    private final BigDecimal amount;        // 결제 금액
    private final String currency;          // 통화 (KRW)
    private final String paymentMethod;     // CARD, CASH 등
    private final String cardNumber;        // 카드번호 (Mock에서는 형식만 검증)
    private final String expiryDate;        // MM/YY
    private final String cvv;               // 3자리
    private final String customerName;      // 고객명
    private final String customerEmail;     // 고객 이메일
}
