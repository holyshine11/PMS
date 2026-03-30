package com.hola.reservation.service;

import com.hola.reservation.booking.gateway.PaymentResult;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.entity.PaymentTransaction;

import java.math.BigDecimal;

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

    /**
     * PG 환불 처리 (PG 결제건이면 KICC API 호출, 아니면 DB 기록만)
     * @return 생성된 REFUND 트랜잭션 (환불 금액이 0이면 null)
     */
    PaymentTransaction processRefundWithPg(Long masterReservationId, BigDecimal refundAmount,
                                            BigDecimal cancelFee, String memo);

    /**
     * PG 환불 재시도 (PG_REFUND_FAILED 상태 거래의 PG 취소 재처리)
     */
    PaymentSummaryResponse retryPgRefund(Long propertyId, Long reservationId, Long transactionId);
}
