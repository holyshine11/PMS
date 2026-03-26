package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.response.EasyPayCardResponse;
import com.hola.reservation.booking.gateway.PaymentResult;

import java.util.List;

/**
 * 간편결제 카드 서비스 인터페이스
 */
public interface EasyPayCardService {

    /** 이메일로 등록된 카드 목록 조회 */
    List<EasyPayCardResponse> getCardsByEmail(String email);

    /** 빌키 발급 결과로 카드 등록 */
    EasyPayCardResponse registerCard(String email, String batchKey, String cardMaskNo,
                                      String issuerName, String cardType, String pgCno);

    /** 카드 삭제 (KICC 빌키 삭제 + DB soft delete) */
    void deleteCard(Long cardId, String email);

    /** 이메일별 카드 등록 가능 여부 (최대 5개) */
    boolean canRegisterMore(String email);
}
