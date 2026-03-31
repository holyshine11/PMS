package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin 예약 취소 수수료 미리보기 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCancelPreviewResponse {

    private Long reservationId;
    private String masterReservationNo;
    /** Leg 단위 취소 시 해당 Leg 예약번호 (전체 취소 시 null) */
    private String subReservationNo;
    private String guestNameKo;
    private String checkIn;
    private String checkOut;
    private String reservationStatus;
    /** 1박 총액 (공급가 + 세액 + 봉사료) */
    private BigDecimal firstNightTotal;
    private BigDecimal cancelFeeAmount;
    private BigDecimal cancelFeePercent;
    private BigDecimal totalPaidAmount;
    private BigDecimal refundAmount;
    /** 미결제 수수료 (cancelFee - totalPaid, 0 이상) — 0보다 크면 상태 변경 차단 */
    private BigDecimal outstandingCancelFee;
    /** 예약 총액 (grandTotal) */
    private BigDecimal grandTotal;
    /** 미결제 잔액 (grandTotal - totalPaid, 0 이상) — 0보다 크면 상태 변경 차단 */
    private BigDecimal unpaidBalance;
    private String policyDescription;

    /** PG 결제 여부 (환불이 PG 경유인지 표시용) */
    private boolean pgPayment;
    /** PG 마스킹 카드번호 */
    private String pgCardNo;
    /** PG 발급사명 */
    private String pgIssuerName;

    /** PG 자동 환불 금액 */
    private BigDecimal pgRefundAmount;
    /** 비-PG(현금/VAN) 수동 환불 금액 */
    private BigDecimal nonPgRefundAmount;
    /** 비-PG 환불 수단 (CASH / CARD) */
    private String nonPgRefundMethod;

    /** 결제수단별 환불 분배 내역 */
    private List<RefundBreakdown> refundBreakdowns;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundBreakdown {
        private String paymentMethod;   // CARD / CASH
        private BigDecimal paidAmount;  // 해당 수단 결제액
        private BigDecimal refundAmount; // 해당 수단 환불 예정액
        private boolean pgRefund;       // PG 자동 환불 여부
        private String cardInfo;        // PG 카드 정보 (있을 때만)
    }
}
