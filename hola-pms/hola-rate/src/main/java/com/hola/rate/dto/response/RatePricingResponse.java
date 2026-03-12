package com.hola.rate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 요금정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatePricingResponse {

    /** 요금 행 목록 */
    private List<PricingRowResponse> pricingRows;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingRowResponse {
        private Long id;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean dayMon;
        private Boolean dayTue;
        private Boolean dayWed;
        private Boolean dayThu;
        private Boolean dayFri;
        private Boolean daySat;
        private Boolean daySun;
        private String currency;
        private BigDecimal baseSupplyPrice;
        private BigDecimal baseTax;
        private BigDecimal baseTotal;

        /** Down/Up sale 설정 (요금 행별) */
        private String downUpSign;
        private BigDecimal downUpValue;
        private String downUpUnit;
        private Integer roundingDecimalPoint;
        private Integer roundingDigits;
        private String roundingMethod;

        private List<PersonPriceResponse> persons;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonPriceResponse {
        private String personType;
        private Integer personSeq;
        private BigDecimal supplyPrice;
        private BigDecimal tax;
        private BigDecimal totalPrice;
    }
}
