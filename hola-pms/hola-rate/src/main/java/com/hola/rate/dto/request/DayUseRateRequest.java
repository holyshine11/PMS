package com.hola.rate.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dayuse 요금 요청 DTO
 */
@Getter
@NoArgsConstructor
public class DayUseRateRequest {

    @NotNull(message = "이용시간은 필수입니다.")
    @Min(value = 1, message = "이용시간은 1시간 이상이어야 합니다.")
    private Integer durationHours;

    @NotNull(message = "요금은 필수입니다.")
    @Min(value = 0, message = "요금은 0 이상이어야 합니다.")
    private BigDecimal supplyPrice;

    private String description;
}
