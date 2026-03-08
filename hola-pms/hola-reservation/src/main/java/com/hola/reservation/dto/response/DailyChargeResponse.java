package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일별 요금 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChargeResponse {

    private Long id;
    private LocalDate chargeDate;
    private BigDecimal supplyPrice;
    private BigDecimal tax;
    private BigDecimal serviceCharge;
    private BigDecimal total;
}
