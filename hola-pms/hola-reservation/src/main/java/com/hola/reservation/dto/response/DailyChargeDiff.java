package com.hola.reservation.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChargeDiff {

    private LocalDate chargeDate;
    private BigDecimal currentCharge;
    private BigDecimal newCharge;
    private BigDecimal difference;
}
