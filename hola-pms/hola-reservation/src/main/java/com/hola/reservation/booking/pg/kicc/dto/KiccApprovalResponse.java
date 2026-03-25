package com.hola.reservation.booking.pg.kicc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KICC 결제승인 응답 / 거래상태 조회 응답
 */
@Getter
@NoArgsConstructor
public class KiccApprovalResponse {

    /** 결과코드 ("0000"=성공) */
    private String resCd;

    /** 결과메시지 */
    private String resMsg;

    /** PG 거래고유번호 (취소/환불 시 필수) */
    private String pgCno;

    /** 결제금액 */
    private Long amount;

    /** 거래일시 (yyyyMMddHHmmss) */
    private String transactionDate;

    /** 거래상태 코드 ("TS01"=미승인, "TS02"=승인취소, "TS03"=매입요청) */
    private String statusCode;

    /** 메시지 인증값 (HmacSHA256) */
    private String msgAuthValue;

    /** 상점 주문번호 */
    private String shopOrderNo;

    /** 결제수단 코드 */
    private String payMethodTypeCode;

    /** 결제 상세 정보 */
    private KiccPaymentInfo paymentInfo;

    public boolean isSuccess() {
        return "0000".equals(resCd);
    }
}
