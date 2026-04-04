package com.hola.reservation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KPSP VAN 응답 페이로드 (브라우저 → 백엔드)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VanResultPayload {

    private boolean success;

    /** 응답 코드 (0000 = 성공) */
    private String respCode;

    /** 응답 텍스트 (approve / denied) */
    private String respText;

    /** 응답 메시지 */
    private String respMessage;

    /** 거래 유형: I1(카드결제), I4(카드취소), B1(현금결제), B2(현금취소) */
    private String transType;

    /** 시퀀스 번호 */
    private String sequenceNo;

    /** 승인 추적번호 */
    private String rrn;

    private String issuerCode;
    private String issuerName;
    private String acquirerCode;
    private String acquirerName;

    /** 마스킹 카드번호 또는 전화번호 */
    private String pan;

    /** 승인번호 */
    private String authCode;

    /** 단말기 ID */
    private String terminalId;

    /** 승인 금액 (교차 검증용) */
    private Long transAmount;
}
