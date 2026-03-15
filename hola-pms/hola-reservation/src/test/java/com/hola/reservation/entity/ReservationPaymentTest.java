package com.hola.reservation.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReservationPayment - 결제 상태 자동 판단")
class ReservationPaymentTest {

    private ReservationPayment createPayment(BigDecimal grandTotal, BigDecimal totalPaid) {
        return ReservationPayment.builder()
                .grandTotal(grandTotal)
                .totalPaidAmount(totalPaid)
                .build();
    }

    @Test
    @DisplayName("grandTotal 0이하 → PAID (결제 불필요)")
    void updatePaymentStatus_zeroGrandTotal_paid() {
        ReservationPayment payment = createPayment(BigDecimal.ZERO, BigDecimal.ZERO);
        payment.updatePaymentStatus();
        assertThat(payment.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("grandTotal 음수 → PAID (조정으로 마이너스)")
    void updatePaymentStatus_negativeGrandTotal_paid() {
        ReservationPayment payment = createPayment(new BigDecimal("-10000"), BigDecimal.ZERO);
        payment.updatePaymentStatus();
        assertThat(payment.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("paid > grandTotal → OVERPAID")
    void updatePaymentStatus_overpaid() {
        ReservationPayment payment = createPayment(new BigDecimal("100000"), new BigDecimal("150000"));
        payment.updatePaymentStatus();
        assertThat(payment.getPaymentStatus()).isEqualTo("OVERPAID");
    }

    @Test
    @DisplayName("paid == grandTotal → PAID")
    void updatePaymentStatus_exactlyPaid() {
        ReservationPayment payment = createPayment(new BigDecimal("100000"), new BigDecimal("100000"));
        payment.updatePaymentStatus();
        assertThat(payment.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("0 < paid < grandTotal → PARTIAL")
    void updatePaymentStatus_partial() {
        ReservationPayment payment = createPayment(new BigDecimal("100000"), new BigDecimal("50000"));
        payment.updatePaymentStatus();
        assertThat(payment.getPaymentStatus()).isEqualTo("PARTIAL");
    }

    @Test
    @DisplayName("paid == 0 → UNPAID")
    void updatePaymentStatus_unpaid() {
        ReservationPayment payment = createPayment(new BigDecimal("100000"), BigDecimal.ZERO);
        payment.updatePaymentStatus();
        assertThat(payment.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test
    @DisplayName("addPaidAmount - 누적 합산 + 상태 자동 갱신")
    void addPaidAmount_accumulatesAndUpdatesStatus() {
        ReservationPayment payment = createPayment(new BigDecimal("200000"), BigDecimal.ZERO);
        payment.updatePaymentStatus();
        assertThat(payment.getPaymentStatus()).isEqualTo("UNPAID");

        // 부분 결제
        payment.addPaidAmount(new BigDecimal("100000"));
        assertThat(payment.getPaymentStatus()).isEqualTo("PARTIAL");
        assertThat(payment.getTotalPaidAmount()).isEqualByComparingTo(new BigDecimal("100000"));

        // 잔액 결제
        payment.addPaidAmount(new BigDecimal("100000"));
        assertThat(payment.getPaymentStatus()).isEqualTo("PAID");
        assertThat(payment.getTotalPaidAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(payment.getPaymentDate()).isNotNull();
    }

    @Test
    @DisplayName("null grandTotal/totalPaid 안전 처리")
    void updatePaymentStatus_nullValues_safe() {
        ReservationPayment payment = ReservationPayment.builder()
                .grandTotal(null)
                .totalPaidAmount(null)
                .build();
        payment.updatePaymentStatus();
        // grandTotal null → 0으로 처리 → PAID (0 이하)
        assertThat(payment.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("updateCancelRefund - 취소 수수료/환불 기록")
    void updateCancelRefund_setsFields() {
        ReservationPayment payment = createPayment(new BigDecimal("100000"), new BigDecimal("100000"));
        payment.updateCancelRefund(new BigDecimal("30000"), new BigDecimal("70000"));

        assertThat(payment.getCancelFeeAmount()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(payment.getRefundAmount()).isEqualByComparingTo(new BigDecimal("70000"));
    }
}
