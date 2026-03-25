package com.hola.reservation.booking.pg.kicc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KICC 카드 결제 상세 (paymentInfo.cardInfo)
 */
@Getter
@NoArgsConstructor
public class KiccCardInfo {

    /** 카드번호 (마스킹 또는 빌키) */
    private String cardNo;

    /** 마스킹 카드번호 (UI 표시용) */
    private String cardMaskNo;

    /** 발급사 코드 */
    private String issuerCd;

    /** 발급사명 */
    private String issuerNm;

    /** 매입사 코드 */
    private String acquirerCd;

    /** 매입사명 */
    private String acquirerNm;

    /** 할부개월 (0=일시불) */
    private Integer installmentMonth;

    /** 카드종류 (신용/체크/기프트) */
    private String cardType;
}
