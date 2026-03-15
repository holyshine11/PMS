package com.hola.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 예약 보증금 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDepositRequest {

    /** 보증금 수단 (필수) */
    @NotBlank(message = "보증금 수단은 필수입니다")
    private String depositMethod;

    private String cardCompany;
    private String cardNumberEncrypted;
    private String cardCvcEncrypted;
    private String cardExpiryDate;
    private String cardPasswordEncrypted;

    /** 통화 코드 (필수) */
    @NotBlank(message = "통화 코드는 필수입니다")
    private String currency;

    /** 보증금 금액 (필수) */
    @NotNull(message = "보증금 금액은 필수입니다")
    @Positive(message = "보증금 금액은 0보다 커야 합니다")
    private BigDecimal amount;
}
