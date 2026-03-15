package com.hola.reservation.booking.exception;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.booking.dto.BookingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 부킹엔진 API 전용 예외 핸들러
 * - /api/v1/booking/** 경로에만 적용
 * - BookingResponse 형식으로 에러 응답
 * - GlobalExceptionHandler보다 우선 처리
 */
@Slf4j
@Order(1)
@RestControllerAdvice(basePackages = "com.hola.reservation.booking.controller")
public class BookingExceptionHandler {

    @ExceptionHandler(HolaException.class)
    public ResponseEntity<BookingResponse<Void>> handleHolaException(HolaException e) {
        log.warn("[Booking] 비즈니스 예외: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BookingResponse.error(errorCode.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BookingResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[Booking] 입력값 검증 실패: {}", message);
        return ResponseEntity
                .badRequest()
                .body(BookingResponse.error(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BookingResponse<Void>> handleException(Exception e) {
        log.error("[Booking] 서버 오류: ", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BookingResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
