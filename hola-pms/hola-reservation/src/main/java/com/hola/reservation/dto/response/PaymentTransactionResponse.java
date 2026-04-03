package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 거래 이력 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionResponse {

    private Long id;
    private Long subReservationId;
    private Integer transactionSeq;
    private String transactionType;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String transactionStatus;
    private String approvalNo;
    private String memo;
    private LocalDateTime createdAt;
    private String createdBy;

    // PG 확장 필드
    private String pgProvider;
    private String pgCno;
    private String pgApprovalNo;
    private String pgCardNo;
    private String pgIssuerName;
    private String pgAcquirerName;
    private Integer pgInstallmentMonth;
    private String pgCardType;

    // VAN 확장 필드
    private String paymentChannel;
    private Long workstationId;
    private String vanAuthCode;
    private String vanIssuerName;
    private String vanPan;
    private String vanAcquirerName;
    private String vanSequenceNo;

    /** 취소 가능 여부 (VAN PAYMENT: 미취소 시퀀스, PG PAYMENT: PG 환불 잔여 용량 있을 때 true) */
    private Boolean cancelable;
}
