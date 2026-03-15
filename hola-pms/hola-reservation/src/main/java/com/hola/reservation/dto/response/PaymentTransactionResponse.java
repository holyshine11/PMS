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
}
