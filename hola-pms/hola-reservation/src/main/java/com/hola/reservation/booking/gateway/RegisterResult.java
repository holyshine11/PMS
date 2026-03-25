package com.hola.reservation.booking.gateway;

import lombok.Builder;
import lombok.Getter;

/**
 * PG 거래등록 결과 DTO
 */
@Getter
@Builder
public class RegisterResult {

    /** 성공 여부 */
    private final boolean success;

    /** 결제창 호출 URL */
    private final String authPageUrl;

    /** 상점 주문번호 */
    private final String orderId;

    /** 실패 시 에러코드 */
    private final String errorCode;

    /** 실패 시 에러메시지 */
    private final String errorMessage;

    public static RegisterResult success(String authPageUrl, String orderId) {
        return RegisterResult.builder()
                .success(true)
                .authPageUrl(authPageUrl)
                .orderId(orderId)
                .build();
    }

    public static RegisterResult failure(String errorCode, String errorMessage) {
        return RegisterResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
