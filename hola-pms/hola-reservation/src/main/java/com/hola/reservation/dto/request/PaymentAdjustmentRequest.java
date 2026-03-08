package com.hola.reservation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 결제 조정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAdjustmentRequest {

    private String adjustmentSign;
    private BigDecimal supplyPrice;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private String comment;
}
