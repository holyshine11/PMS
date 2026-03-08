package com.hola.reservation.dto.request;

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

    private String depositMethod;
    private String cardCompany;
    private String cardNumberEncrypted;
    private String cardCvcEncrypted;
    private String cardExpiryDate;
    private String cardPasswordEncrypted;
    private String currency;
    private BigDecimal amount;
}
