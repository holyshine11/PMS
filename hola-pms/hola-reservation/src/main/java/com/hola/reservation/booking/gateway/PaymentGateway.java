package com.hola.reservation.booking.gateway;

/**
 * 결제 게이트웨이 추상화 인터페이스
 * - 현재: MockPaymentGateway (가상 승인)
 * - 향후: KICCPaymentGateway, PayPalPaymentGateway로 교체
 */
public interface PaymentGateway {

    /**
     * 결제 승인 요청
     */
    PaymentResult authorize(PaymentRequest request);

    /**
     * 승인 취소 (결제 실패 시 롤백용)
     */
    PaymentResult cancel(String approvalNo);

    /**
     * 게이트웨이 식별자
     */
    String getGatewayId();
}
