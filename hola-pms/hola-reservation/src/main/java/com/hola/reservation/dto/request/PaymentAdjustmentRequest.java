package com.hola.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    /** 조정 부호: +/- (필수) */
    @NotBlank(message = "조정 부호는 필수입니다")
    private String adjustmentSign;

    /** 공급가 (필수) */
    @NotNull(message = "공급가는 필수입니다")
    private BigDecimal supplyPrice;

    /** 세금 */
    private BigDecimal tax;

    /** 총액 (필수) */
    @NotNull(message = "총액은 필수입니다")
    private BigDecimal totalAmount;

    /** 사유 */
    private String comment;
}
