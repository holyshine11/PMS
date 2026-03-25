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

    // === PG 확장 필드 ===
    private final String pgCno;             // PG 거래고유번호
    private final String pgProvider;        // PG 제공자 ("KICC", "MOCK")
    private final String pgStatusCode;      // PG 거래상태 코드
    private final String pgApprovalNo;      // PG 승인번호
    private final String pgApprovalDate;    // PG 승인일시 (yyyyMMddHHmmss)
    private final String pgCardNo;          // 마스킹 카드번호
    private final String pgIssuerName;      // 발급사명
    private final String pgAcquirerName;    // 매입사명
    private final Integer installmentMonth; // 할부개월 (0=일시불)
    private final String pgCardType;        // 카드종류 (신용/체크)
    private final String pgRawResponse;     // 원본 응답 JSON (감사추적용)
    private final String pgTransactionId;   // 멱등성 키 (UUID)

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
