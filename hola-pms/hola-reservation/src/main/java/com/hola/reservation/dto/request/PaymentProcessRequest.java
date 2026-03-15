package com.hola.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
    @NotBlank(message = "결제 수단은 필수입니다")
    private String paymentMethod;

    /** 결제 금액 (null이면 잔액 전액) */
    @Positive(message = "결제 금액은 0보다 커야 합니다")
    private BigDecimal amount;

    /** 메모 (선택) */
    private String memo;
}
