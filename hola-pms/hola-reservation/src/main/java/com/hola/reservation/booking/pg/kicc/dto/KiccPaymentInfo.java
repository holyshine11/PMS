package com.hola.reservation.booking.pg.kicc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KICC 결제 상세 정보 (승인 응답 내 paymentInfo)
 */
@Getter
@NoArgsConstructor
public class KiccPaymentInfo {

    /** 승인번호 */
    private String approvalNo;

    /** 카드 결제 상세 */
    private KiccCardInfo cardInfo;
}
