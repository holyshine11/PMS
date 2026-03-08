package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 조정 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAdjustmentResponse {

    private Long id;
    private Integer adjustmentSeq;
    private String currency;
    private String adjustmentSign;
    private BigDecimal supplyPrice;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private String comment;
    private LocalDateTime createdAt;
    private String createdBy;
}
