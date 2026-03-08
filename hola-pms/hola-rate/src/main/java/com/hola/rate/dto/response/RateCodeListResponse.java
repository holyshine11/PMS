package com.hola.rate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 레이트 코드 리스트 응답 DTO (간소화)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCodeListResponse {

    private Long id;
    private Long propertyId;
    private String rateCode;
    private String rateNameKo;
    private String rateCategory;
    private String marketCodeName;
    private String currency;
    private LocalDate saleStartDate;
    private LocalDate saleEndDate;
    private long roomTypeCount;
    private Boolean useYn;
    private LocalDateTime updatedAt;
}
