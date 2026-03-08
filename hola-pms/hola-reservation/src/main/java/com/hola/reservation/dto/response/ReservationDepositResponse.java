package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 예약 보증금 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDepositResponse {

    private Long id;
    private String depositMethod;
    private String cardCompany;
    private String cardNumberMasked;
    private String cardExpiryDate;
    private String currency;
    private BigDecimal amount;
}
