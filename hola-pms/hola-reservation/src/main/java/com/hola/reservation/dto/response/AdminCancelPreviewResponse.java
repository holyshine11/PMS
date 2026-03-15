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
    private BigDecimal firstNightSupplyPrice;
    private BigDecimal cancelFeeAmount;
    private BigDecimal cancelFeePercent;
    private BigDecimal totalPaidAmount;
    private BigDecimal refundAmount;
    private String policyDescription;
}
