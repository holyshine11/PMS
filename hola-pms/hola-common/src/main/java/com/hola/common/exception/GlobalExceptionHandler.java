package com.hola.common.exception;

import com.hola.common.dto.HolaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HolaException.class)
    public ResponseEntity<HolaResponse<Void>> handleHolaException(HolaException e) {
        log.warn("비즈니스 예외: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(HolaResponse.error(errorCode.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HolaResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("입력값 검증 실패: {}", message);
        return ResponseEntity
                .badRequest()
                .body(HolaResponse.error(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<HolaResponse<Void>> handleOptimisticLockException(ObjectOptimisticLockingFailureException e) {
        log.warn("동시성 충돌 발생: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(HolaResponse.error("HOLA-4027", "다른 요청이 진행 중입니다. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<HolaResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        // favicon.ico, .well-known 등 브라우저 자동 요청은 무시
        log.debug("정적 리소스 없음: {}", e.getResourcePath());
        return ResponseEntity
                .status(404)
                .body(HolaResponse.error("HOLA-0004", "리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HolaResponse<Void>> handleException(Exception e) {
        log.error("서버 오류: ", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(HolaResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
