package com.hola.reservation.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeAvailableTypeResponse {

    private Long roomTypeId;
    private String roomTypeCode;
    private String description;
    private Integer maxAdults;
    private Integer maxChildren;
    private BigDecimal estimatedPriceDifference;    // 대략적 차액 (일일 기준)
}
