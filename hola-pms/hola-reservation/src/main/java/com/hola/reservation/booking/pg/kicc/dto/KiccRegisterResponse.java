package com.hola.reservation.booking.pg.kicc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KICC 거래등록 응답
 */
@Getter
@NoArgsConstructor
public class KiccRegisterResponse {

    /** 결과코드 ("0000"=성공) */
    private String resCd;

    /** 결과메시지 */
    private String resMsg;

    /** 결제창 호출 URL */
    private String authPageUrl;

    public boolean isSuccess() {
        return "0000".equals(resCd);
    }
}
