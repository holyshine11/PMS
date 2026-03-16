package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Card BIN 검증 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class CardBinValidationResponse {

    /** 입력된 BIN */
    private final String bin;

    /** 카드 네트워크 (VISA, MASTERCARD, AMEX, JCB, UNIONPAY, UNKNOWN) */
    private final String network;

    /** 카드 네트워크 표시명 */
    private final String displayName;

    /** 카드 타입 (CREDIT, DEBIT) */
    private final String cardType;

    /** 지원 여부 */
    private final boolean supported;
}
