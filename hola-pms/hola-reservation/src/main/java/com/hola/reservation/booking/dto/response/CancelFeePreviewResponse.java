package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 취소 수수료 미리보기 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelFeePreviewResponse {

    /** 확인번호 */
    private String confirmationNo;

    /** 예약 상태 */
    private String reservationStatus;

    /** 투숙객명 */
    private String guestNameKo;

    /** 체크인 날짜 */
    private String checkIn;

    /** 체크아웃 날짜 */
    private String checkOut;

    /** 1박 객실 요금 (공급가 + 세액 + 봉사료) */
    private BigDecimal firstNightAmount;

    /** 취소 수수료 금액 */
    private BigDecimal cancelFeeAmount;

    /** 취소 수수료 비율 (%) */
    private BigDecimal cancelFeePercent;

    /** 총 결제 금액 */
    private BigDecimal totalPaidAmount;

    /** 환불 예정 금액 */
    private BigDecimal refundAmount;

    /** 정책 설명 */
    private String policyDescription;

    /** PG 결제 여부 */
    private boolean pgPayment;
    /** PG 마스킹 카드번호 */
    private String pgCardNo;
    /** PG 발급사명 */
    private String pgIssuerName;
}
