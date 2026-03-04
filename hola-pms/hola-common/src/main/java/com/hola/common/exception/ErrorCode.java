package com.hola.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 체계
 * HOLA-0xxx: 공통
 * HOLA-1xxx: 호텔/프로퍼티
 * HOLA-2xxx: 객실
 * HOLA-3xxx: 레이트
 * HOLA-06xx: 회원관리
 * HOLA-7xxx: 예약
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 (HOLA-0xxx)
    INTERNAL_SERVER_ERROR("HOLA-0001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("HOLA-0002", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("HOLA-0003", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE("HOLA-0004", "이미 존재하는 리소스입니다.", HttpStatus.CONFLICT),
    UNAUTHORIZED("HOLA-0005", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("HOLA-0006", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("HOLA-0007", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("HOLA-0008", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),

    // 호텔/프로퍼티 (HOLA-1xxx)
    HOTEL_NOT_FOUND("HOLA-1000", "호텔을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    HOTEL_CODE_DUPLICATE("HOLA-1001", "이미 존재하는 호텔 코드입니다.", HttpStatus.CONFLICT),
    HOTEL_HAS_PROPERTIES("HOLA-1002", "하위 프로퍼티가 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    HOTEL_NAME_DUPLICATE("HOLA-1003", "이미 존재하는 호텔명입니다.", HttpStatus.CONFLICT),
    CANNOT_DELETE_ACTIVE("HOLA-1004", "사용 중인 항목은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    PROPERTY_NOT_FOUND("HOLA-1010", "프로퍼티를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PROPERTY_CODE_DUPLICATE("HOLA-1011", "동일 호텔 내 이미 존재하는 프로퍼티 코드입니다.", HttpStatus.CONFLICT),
    PROPERTY_NAME_DUPLICATE("HOLA-1012", "동일 호텔 내 이미 존재하는 프로퍼티명입니다.", HttpStatus.CONFLICT),
    PROPERTY_HAS_CHILDREN("HOLA-1013", "하위 데이터가 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    FLOOR_NOT_FOUND("HOLA-1020", "층 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FLOOR_NUMBER_DUPLICATE("HOLA-1021", "동일 프로퍼티 내 이미 존재하는 층 번호입니다.", HttpStatus.CONFLICT),
    FLOOR_HAS_ROOMS("HOLA-1022", "하위 호수가 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ROOM_NUMBER_NOT_FOUND("HOLA-1030", "호수 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROOM_NUMBER_DUPLICATE("HOLA-1031", "동일 프로퍼티 내 이미 존재하는 호수입니다.", HttpStatus.CONFLICT),
    MARKET_CODE_NOT_FOUND("HOLA-1040", "마켓코드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MARKET_CODE_DUPLICATE("HOLA-1041", "동일 프로퍼티 내 이미 존재하는 마켓코드입니다.", HttpStatus.CONFLICT),
    SETTLEMENT_NOT_FOUND("HOLA-1050", "정산정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 회원관리 (HOLA-06xx)
    ADMIN_NOT_FOUND("HOLA-0600", "관리자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ADMIN_LOGIN_ID_DUPLICATE("HOLA-0601", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT),
    ADMIN_PROPERTY_REQUIRED("HOLA-0602", "프로퍼티를 1개 이상 선택해야 합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
