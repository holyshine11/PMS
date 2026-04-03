package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.response.BookingConfirmationResponse;
import com.hola.reservation.booking.dto.response.EasyPayCardResponse;
import com.hola.reservation.booking.gateway.RegisterResult;

import java.util.List;
import java.util.Map;

/**
 * 간편결제(빌키) 비즈니스 서비스 인터페이스
 * - 빌키 등록/결제/결과조회 등 PG 연동 로직 포함
 */
public interface EasyPayService {

    /**
     * 빌키 등록 - Step 1: 거래등록 → KICC 인증창 URL 반환
     * @param email 고객 이메일
     * @param customerName 고객명
     * @param customerPhone 고객 연락처
     * @param deviceType PC/모바일 구분 ("pc" or "mobile")
     * @return 인증페이지 URL + shopOrderNo 포함 Map
     */
    Map<String, Object> registerBillkey(String email, String customerName, String customerPhone, String deviceType);

    /**
     * 빌키 등록 - Step 2: KICC returnUrl 콜백 → 빌키 발급 + DB 저장
     * @param resCd KICC 응답코드
     * @param shopOrderNo 주문번호
     * @param authorizationId 인증ID
     * @param resMsg 응답메시지
     * @return 결과 Map (success, errorMessage, shopOrderNo)
     */
    Map<String, Object> processBillkeyReturn(String resCd, String shopOrderNo, String authorizationId, String resMsg);

    /**
     * 빌키 등록 결과 조회 (폴링용)
     * @param shopOrderNo 주문번호
     * @return 결과 Map (status, success, card)
     */
    Map<String, Object> getBillkeyResult(String shopOrderNo);

    /**
     * 간편결제로 예약 생성 (빌키 결제)
     * @param propertyCode 프로퍼티 코드
     * @param cardId 카드 ID
     * @param request 예약 생성 요청
     * @param clientIp 클라이언트 IP
     * @param userAgent User-Agent
     * @return 결제+예약 결과 Map
     */
    Map<String, Object> payWithBillkey(String propertyCode, Long cardId, BookingCreateRequest request,
                                        String clientIp, String userAgent);
}
