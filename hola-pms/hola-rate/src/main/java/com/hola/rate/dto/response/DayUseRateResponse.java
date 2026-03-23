package com.hola.rate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dayuse 요금 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayUseRateResponse {

    private Long id;
    private Long rateCodeId;
    private Integer durationHours;
    private BigDecimal supplyPrice;
    private String description;
    private Integer sortOrder;
    private Boolean useYn;
}
