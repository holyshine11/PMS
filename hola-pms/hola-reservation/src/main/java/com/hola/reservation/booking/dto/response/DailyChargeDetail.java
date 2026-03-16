package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일자별 요금 상세 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyChargeDetail {

    private LocalDate chargeDate;
    private BigDecimal supplyPrice;
    private BigDecimal tax;
    private BigDecimal serviceCharge;
    private BigDecimal total;
}
