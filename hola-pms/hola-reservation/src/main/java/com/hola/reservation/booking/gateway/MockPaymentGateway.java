package com.hola.reservation.booking.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock 결제 게이트웨이
 * - 항상 결제 성공 처리
 * - 향후 KICC/PayPal 연동 시 이 클래스를 대체
 */
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String GATEWAY_ID = "MOCK";

    @Override
    public PaymentResult authorize(PaymentRequest request) {
        String approvalNo = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[MockPG] 결제 승인 - orderId: {}, amount: {} {}, approvalNo: {}",
                request.getOrderId(), request.getAmount(), request.getCurrency(), approvalNo);

        return PaymentResult.success(approvalNo, GATEWAY_ID, request.getAmount());
    }

    @Override
    public PaymentResult cancel(String approvalNo) {
        log.info("[MockPG] 결제 취소 - approvalNo: {}", approvalNo);

        return PaymentResult.builder()
                .success(true)
                .approvalNo(approvalNo)
                .gatewayId(GATEWAY_ID)
                .build();
    }

    @Override
    public String getGatewayId() {
        return GATEWAY_ID;
    }
}
