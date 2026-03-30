package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    private String policyDescription;

    /** PG 결제 여부 (환불이 PG 경유인지 표시용) */
    private boolean pgPayment;
    /** PG 마스킹 카드번호 */
    private String pgCardNo;
    /** PG 발급사명 */
    private String pgIssuerName;
}
