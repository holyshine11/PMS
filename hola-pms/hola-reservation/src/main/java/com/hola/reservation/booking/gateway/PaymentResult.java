package com.hola.reservation.booking.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 게이트웨이 응답 DTO
 */
@Getter
@Builder
public class PaymentResult {

    private final boolean success;
    private final String approvalNo;        // 승인번호
    private final String gatewayId;         // 게이트웨이 식별자
    private final BigDecimal amount;        // 승인 금액
    private final String errorCode;         // 실패 시 에러코드
    private final String errorMessage;      // 실패 시 에러메시지
    private final LocalDateTime processedAt;

    public static PaymentResult success(String approvalNo, String gatewayId, BigDecimal amount) {
        return PaymentResult.builder()
                .success(true)
                .approvalNo(approvalNo)
                .gatewayId(gatewayId)
                .amount(amount)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static PaymentResult failure(String gatewayId, String errorCode, String errorMessage) {
        return PaymentResult.builder()
                .success(false)
                .gatewayId(gatewayId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
