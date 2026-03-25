package com.hola.reservation.service;

import com.hola.reservation.booking.gateway.PaymentResult;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;

/**
 * 예약 결제 서비스 인터페이스
 */
public interface ReservationPaymentService {

    /** 결제 정보 조회 */
    PaymentSummaryResponse getPaymentSummary(Long propertyId, Long reservationId);

    /** 결제 처리 (카드/현금, 부분결제 지원) */
    PaymentSummaryResponse processPayment(Long propertyId, Long reservationId, PaymentProcessRequest request);

    /** PG 결제 결과 포함 결제 처리 (KICC 등 PG 연동) */
    PaymentSummaryResponse processPaymentWithPgResult(Long propertyId, Long reservationId,
                                                       PaymentProcessRequest request, PaymentResult pgResult);

    /** 금액 조정 추가 */
    PaymentAdjustmentResponse addAdjustment(Long propertyId, Long reservationId, PaymentAdjustmentRequest request);

    /** 결제 금액 재계산 */
    void recalculatePayment(Long reservationId);
}
