package com.hola.reservation.booking.gateway;

/**
 * 결제 게이트웨이 추상화 인터페이스
 * - MockPaymentGateway: 테스트용 가상 승인 (동기식)
 * - KiccPaymentGateway: KICC 이지페이 실제 PG (3단계 비동기)
 */
public interface PaymentGateway {

    /**
     * 결제 승인 요청 (동기식 — Mock용)
     */
    PaymentResult authorize(PaymentRequest request);

    /**
     * 승인 취소 (결제 실패 시 롤백용 — Mock용)
     */
    PaymentResult cancel(String approvalNo);

    /**
     * 게이트웨이 식별자
     */
    String getGatewayId();

    // === PG 3단계 플로우 (거래등록 → 결제창 → 승인) ===

    /**
     * PG 거래등록 — 결제창 URL 반환
     * @return authPageUrl을 포함한 등록 결과
     */
    default RegisterResult registerTransaction(RegisterRequest request) {
        throw new UnsupportedOperationException("PG 거래등록 미지원: " + getGatewayId());
    }

    /**
     * PG 인증 후 결제승인 — 결제창에서 받은 authorizationId로 최종 승인
     */
    default PaymentResult approveAfterAuth(ApproveAfterAuthRequest request) {
        throw new UnsupportedOperationException("PG 인증 후 승인 미지원: " + getGatewayId());
    }

    /**
     * PG 결제 취소 (전체/부분)
     */
    default PaymentResult cancelPayment(CancelPaymentRequest request) {
        throw new UnsupportedOperationException("PG 결제 취소 미지원: " + getGatewayId());
    }
}
