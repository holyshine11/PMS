package com.hola.common.exception;

import com.hola.common.dto.HolaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HolaResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(HolaResponse.error(ErrorCode.INVALID_INPUT.getCode(), "입력값 형식이 올바르지 않습니다."));
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

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class})
    public ResponseEntity<HolaResponse<Void>> handleOptimisticLockException(Exception e) {
        log.warn("동시성 충돌 발생: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(HolaResponse.error("HOLA-4027", "다른 요청이 진행 중입니다. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<HolaResponse<Void>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("데이터 무결성 위반: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(HolaResponse.error("HOLA-0004", "중복된 데이터가 존재합니다."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<HolaResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getParameterName());
        return ResponseEntity
                .badRequest()
                .body(HolaResponse.error(ErrorCode.INVALID_INPUT.getCode(),
                        "필수 파라미터가 누락되었습니다: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<HolaResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 오류: {} = {}", e.getName(), e.getValue());
        return ResponseEntity
                .badRequest()
                .body(HolaResponse.error(ErrorCode.INVALID_INPUT.getCode(),
                        "잘못된 파라미터 형식입니다: " + e.getName()));
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
