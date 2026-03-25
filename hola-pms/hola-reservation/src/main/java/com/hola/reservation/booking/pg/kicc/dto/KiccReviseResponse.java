package com.hola.reservation.booking.pg.kicc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KICC 결제 취소/환불 응답
 */
@Getter
@NoArgsConstructor
public class KiccReviseResponse {

    /** 결과코드 ("0000"=성공) */
    private String resCd;

    /** 결과메시지 */
    private String resMsg;

    /** 원거래 PG 번호 */
    private String oriPgCno;

    /** 취소 PG 번호 */
    private String cancelPgCno;

    /** 거래상태 코드 ("TS02"=승인취소) */
    private String statusCode;

    /** 취소금액 */
    private Long cancelAmount;

    /** 잔여금액 */
    private Long remainAmount;

    /** 취소 상세 정보 */
    private ReviseInfo reviseInfo;

    public boolean isSuccess() {
        return "0000".equals(resCd);
    }

    @Getter
    @NoArgsConstructor
    public static class ReviseInfo {
        /** 취소 승인번호 */
        private String approvalNo;
    }
}
