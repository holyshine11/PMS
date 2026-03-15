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
 * HOLA-4xxx: 예약
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
    ADMIN_PROPERTY_REQUIRED("HOLA-0602", "프로퍼티를 1개 이상 선택해야 합니다.", HttpStatus.BAD_REQUEST),

    // 비밀번호 (HOLA-08xx)
    PASSWORD_MISMATCH("HOLA-0800", "현재 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRM_MISMATCH("HOLA-0801", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID_FORMAT("HOLA-0802", "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.", HttpStatus.BAD_REQUEST),

    // 권한관리 (HOLA-07xx)
    ROLE_NOT_FOUND("HOLA-0700", "권한을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROLE_NAME_DUPLICATE("HOLA-0701", "동일 호텔 내 이미 존재하는 권한명입니다.", HttpStatus.CONFLICT),
    ROLE_HAS_USERS("HOLA-0702", "해당 권한을 사용하는 관리자가 있어 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ROLE_PROPERTY_ALREADY_EXISTS("HOLA-0703", "해당 프로퍼티에 이미 권한이 설정되어 있습니다.", HttpStatus.CONFLICT),

    // 객실관리 (HOLA-2xxx)
    ROOM_CLASS_NOT_FOUND("HOLA-2000", "객실 클래스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROOM_CLASS_CODE_DUPLICATE("HOLA-2001", "동일 프로퍼티 내 이미 존재하는 객실 클래스 코드입니다.", HttpStatus.CONFLICT),
    ROOM_CLASS_HAS_ROOM_TYPES("HOLA-2002", "하위 객실 타입이 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ROOM_TYPE_NOT_FOUND("HOLA-2010", "객실 타입을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROOM_TYPE_CODE_DUPLICATE("HOLA-2011", "동일 프로퍼티 내 이미 존재하는 객실 타입 코드입니다.", HttpStatus.CONFLICT),
    FREE_SERVICE_OPTION_NOT_FOUND("HOLA-2020", "무료 서비스 옵션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FREE_SERVICE_CODE_DUPLICATE("HOLA-2021", "동일 프로퍼티 내 이미 존재하는 서비스 옵션 코드입니다.", HttpStatus.CONFLICT),
    PAID_SERVICE_OPTION_NOT_FOUND("HOLA-2030", "유료 서비스 옵션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PAID_SERVICE_CODE_DUPLICATE("HOLA-2031", "동일 프로퍼티 내 이미 존재하는 서비스 옵션 코드입니다.", HttpStatus.CONFLICT),

    // 레이트관리 (HOLA-3xxx)
    RATE_CODE_NOT_FOUND("HOLA-3000", "레이트 코드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    RATE_CODE_DUPLICATE("HOLA-3001", "동일 프로퍼티 내 이미 존재하는 레이트 코드입니다.", HttpStatus.CONFLICT),
    RATE_CODE_HAS_PRICING("HOLA-3002", "하위 요금 정보가 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    RATE_INVALID_STAY_DAYS("HOLA-3003", "최대 숙박일수는 최소 숙박일수보다 크거나 같아야 합니다.", HttpStatus.BAD_REQUEST),
    RATE_INVALID_SALE_PERIOD("HOLA-3004", "판매 종료일은 판매 시작일보다 같거나 이후여야 합니다.", HttpStatus.BAD_REQUEST),
    RATE_PRICING_PERIOD_REQUIRED("HOLA-3005", "요금 설정 기간은 필수 입력 항목입니다.", HttpStatus.BAD_REQUEST),
    RATE_INVALID_PRICING_PERIOD("HOLA-3006", "요금 설정 기간의 종료일은 시작일보다 같거나 이후여야 합니다.", HttpStatus.BAD_REQUEST),
    RATE_PRICING_PERIOD_OUT_OF_SALE("HOLA-3007", "요금 설정 기간은 판매기간 내에 있어야 합니다.", HttpStatus.BAD_REQUEST),
    RATE_PRICING_PERIOD_OVERLAP("HOLA-3008", "요금 설정 기간이 다른 요금과 중복됩니다.", HttpStatus.BAD_REQUEST),

    // 프로모션 코드 (HOLA-35xx)
    PROMOTION_CODE_NOT_FOUND("HOLA-3500", "프로모션 코드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PROMOTION_CODE_DUPLICATE("HOLA-3501", "동일 프로퍼티 내 이미 존재하는 프로모션 코드입니다.", HttpStatus.CONFLICT),
    PROMOTION_INVALID_PERIOD("HOLA-3502", "프로모션 종료일은 시작일보다 같거나 이후여야 합니다.", HttpStatus.BAD_REQUEST),

    // 예약관리 (HOLA-4xxx)
    RESERVATION_NOT_FOUND("HOLA-4000", "예약을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    RESERVATION_ALREADY_EXISTS("HOLA-4001", "이미 존재하는 예약번호입니다.", HttpStatus.CONFLICT),
    RESERVATION_INVALID_STATUS("HOLA-4002", "유효하지 않은 예약 상태입니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_STATUS_CHANGE_NOT_ALLOWED("HOLA-4003", "해당 상태로 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 서브 예약 (HOLA-401x)
    SUB_RESERVATION_NOT_FOUND("HOLA-4010", "객실 예약을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SUB_RESERVATION_ROOM_CONFLICT("HOLA-4011", "해당 기간에 이미 예약된 객실입니다.", HttpStatus.CONFLICT),
    SUB_RESERVATION_NO_AVAILABILITY("HOLA-4012", "해당 객실 타입의 가용 객실이 없습니다.", HttpStatus.BAD_REQUEST),
    SUB_RESERVATION_DATE_INVALID("HOLA-4013", "체크아웃은 체크인 이후여야 합니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_CHECKIN_PAST_DATE("HOLA-4014", "체크인 날짜는 오늘 이후여야 합니다.", HttpStatus.BAD_REQUEST),

    // 가격/결제 (HOLA-402x)
    RESERVATION_RATE_NOT_APPLICABLE("HOLA-4020", "해당 기간에 적용 가능한 요금이 없습니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_PAYMENT_ALREADY_COMPLETED("HOLA-4021", "이미 결제 완료된 예약입니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_RATE_EXPIRED("HOLA-4022", "레이트코드의 판매 기간이 예약 기간과 맞지 않습니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_STAY_DAYS_VIOLATION("HOLA-4023", "숙박 일수가 레이트코드의 최소/최대 범위를 벗어납니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_RATE_REQUIRED("HOLA-4024", "레이트코드를 선택해야 예약을 생성할 수 있습니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_PAYMENT_MODIFY_NOT_ALLOWED("HOLA-4025", "완료된 결제는 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 예약채널 (HOLA-403x)
    RESERVATION_CHANNEL_NOT_FOUND("HOLA-4030", "예약채널을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    RESERVATION_CHANNEL_CODE_DUPLICATE("HOLA-4031", "이미 존재하는 채널 코드입니다.", HttpStatus.CONFLICT),

    // 예약 수정 제한 (HOLA-404x)
    RESERVATION_OTA_EDIT_RESTRICTED("HOLA-4040", "OTA 예약은 수정이 제한됩니다.", HttpStatus.FORBIDDEN),
    RESERVATION_MODIFY_NOT_ALLOWED("HOLA-4041", "해당 상태에서는 예약을 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    SUB_RESERVATION_MASTER_MISMATCH("HOLA-4042", "해당 예약에 속하지 않는 객실 레그입니다.", HttpStatus.BAD_REQUEST),
    DEPOSIT_NOT_FOUND("HOLA-4043", "예치금 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 예약 삭제 (HOLA-4044~4045)
    RESERVATION_DELETE_NOT_ALLOWED("HOLA-4044", "체크아웃 상태의 예약만 삭제할 수 있습니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_DELETE_UNAUTHORIZED("HOLA-4045", "예약 삭제는 슈퍼어드민만 가능합니다.", HttpStatus.FORBIDDEN),

    // 예약 서비스 (HOLA-406x)
    RESERVATION_SERVICE_NOT_FOUND("HOLA-4060", "예약 서비스 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    RESERVATION_SERVICE_MISMATCH("HOLA-4061", "해당 객실 레그에 속하지 않는 서비스 항목입니다.", HttpStatus.BAD_REQUEST),

    // 얼리체크인/레이트체크아웃 (HOLA-405x)
    EARLY_LATE_POLICY_NOT_FOUND("HOLA-4050", "얼리/레이트 정책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    EARLY_LATE_INVALID_TIME_RANGE("HOLA-4051", "시작 시간이 종료 시간보다 이후일 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 결제 처리 (HOLA-402x 확장)
    RESERVATION_PAYMENT_AMOUNT_EXCEEDED("HOLA-4026", "결제 금액이 잔액을 초과합니다.", HttpStatus.BAD_REQUEST),
    RESERVATION_PAYMENT_AMOUNT_INVALID("HOLA-4028", "결제 금액은 0보다 커야 합니다.", HttpStatus.BAD_REQUEST),

    // 부킹엔진 (HOLA-407x)
    BOOKING_PROPERTY_NOT_FOUND("HOLA-4070", "예약 가능한 프로퍼티를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BOOKING_NO_AVAILABILITY("HOLA-4071", "선택한 기간에 예약 가능한 객실이 없습니다.", HttpStatus.CONFLICT),
    BOOKING_RATE_NOT_AVAILABLE("HOLA-4072", "선택한 기간에 적용 가능한 요금이 없습니다.", HttpStatus.BAD_REQUEST),
    BOOKING_PAYMENT_FAILED("HOLA-4073", "결제 처리에 실패했습니다.", HttpStatus.BAD_REQUEST),
    BOOKING_DUPLICATE_REQUEST("HOLA-4074", "이미 처리된 예약 요청입니다.", HttpStatus.CONFLICT),
    BOOKING_TERMS_NOT_AGREED("HOLA-4075", "이용약관에 동의해야 합니다.", HttpStatus.BAD_REQUEST),
    BOOKING_INVALID_DATE_RANGE("HOLA-4076", "체크인/체크아웃 날짜가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    BOOKING_MAX_STAY_EXCEEDED("HOLA-4077", "최대 숙박 가능 일수(30일)를 초과했습니다.", HttpStatus.BAD_REQUEST),
    BOOKING_CONFIRMATION_NOT_FOUND("HOLA-4078", "예약 확인 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BOOKING_GUEST_VERIFICATION_FAILED("HOLA-4079", "예약자 정보가 일치하지 않습니다.", HttpStatus.FORBIDDEN),

    // 취소 정책 (HOLA-408x)
    BOOKING_CANCEL_NOT_ALLOWED("HOLA-4080", "해당 상태에서는 취소할 수 없습니다.", HttpStatus.BAD_REQUEST),
    BOOKING_ALREADY_CANCELED("HOLA-4081", "이미 취소된 예약입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
