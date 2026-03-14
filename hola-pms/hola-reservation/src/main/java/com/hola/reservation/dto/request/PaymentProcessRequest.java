package com.hola.reservation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 결제 처리 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessRequest {

    /** 결제 수단: CARD, CASH (필수) */
    private String paymentMethod;

    /** 결제 금액 (null이면 잔액 전액) */
    private BigDecimal amount;

    /** 메모 (선택) */
    private String memo;
}
