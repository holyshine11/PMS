package com.hola.rate.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 요금정보 저장 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RatePricingRequest {

    /** 요금 행 목록 */
    private List<PricingRow> pricingRows;

    @Getter
    @NoArgsConstructor
    public static class PricingRow {
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

        /** 인원별 추가 요금 */
        private List<PersonPrice> persons;
    }

    @Getter
    @NoArgsConstructor
    public static class PersonPrice {
        private String personType;
        private Integer personSeq;
        private BigDecimal supplyPrice;
        private BigDecimal tax;
        private BigDecimal totalPrice;
    }
}
