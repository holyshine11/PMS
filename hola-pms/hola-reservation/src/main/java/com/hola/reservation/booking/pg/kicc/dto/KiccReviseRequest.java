package com.hola.reservation.booking.pg.kicc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * KICC 결제 취소/환불 요청 (POST /api/trades/revise)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiccReviseRequest {

    /** KICC 상점ID */
    private String mallId;

    /** 멱등성 키 (UUID) */
    private String shopTransactionId;

    /** 원거래 PG 거래고유번호 */
    private String pgCno;

    /** 변경구분 코드 ("40"=전체취소, "32"=카드부분취소) */
    private String reviseTypeCode;

    /** 취소요청일 (yyyyMMdd) */
    private String cancelReqDate;

    /** 메시지 인증값 (HmacSHA256: pgCno + "|" + shopTransactionId) */
    private String msgAuthValue;

    /** 취소 사유 */
    private String reviseMessage;

    /** 취소 금액 (부분취소 시) */
    private Long amount;

    /** 취소 후 잔액 (부분취소 시 검증용) */
    private Long remainAmount;

    /** 환불 정보 (계좌이체/가상계좌 환불 시) */
    private RefundInfo refundInfo;

    /** 환불 하위 유형 코드 */
    private String reviseSubTypeCode;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RefundInfo {
        private String refundBankCode;
        private String refundAccountNo;
        private String refundDepositName;
    }
}
