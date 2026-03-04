package com.hola.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외
 */
@Getter
public class HolaException extends RuntimeException {

    private final ErrorCode errorCode;

    public HolaException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public HolaException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
