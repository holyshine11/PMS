package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 요약 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummaryResponse {

    private Long id;
    private String paymentStatus;
    private BigDecimal totalRoomAmount;
    private BigDecimal totalServiceAmount;
    private BigDecimal totalServiceChargeAmount;
    private BigDecimal totalAdjustmentAmount;
    private BigDecimal totalEarlyLateFee;
    private BigDecimal grandTotal;
    private BigDecimal totalPaidAmount;
    private BigDecimal cancelFeeAmount;
    private BigDecimal refundAmount;
    private BigDecimal remainingAmount;
    private LocalDateTime paymentDate;
    private String paymentMethod;

    // 결제 조정 내역
    private List<PaymentAdjustmentResponse> adjustments;

    // 결제 거래 이력
    private List<PaymentTransactionResponse> transactions;
}
