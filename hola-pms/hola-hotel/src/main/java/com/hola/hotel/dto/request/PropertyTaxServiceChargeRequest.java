package com.hola.hotel.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * TAX/봉사료 정보 저장 요청
 */
@Getter
@NoArgsConstructor
public class PropertyTaxServiceChargeRequest {

    @NotNull(message = "TAX 비율은 필수입니다.")
    @DecimalMin(value = "0", message = "TAX 비율은 0 이상이어야 합니다.")
    @DecimalMax(value = "100", message = "TAX 비율은 100 이하이어야 합니다.")
    private BigDecimal taxRate;

    @NotNull(message = "TAX 소수점 자릿수는 필수입니다.")
    @Min(value = 0, message = "소수점 자릿수는 0 이상이어야 합니다.")
    private Integer taxDecimalPlaces;

    private String taxRoundingMethod;

    // 봉사료 타입: PERCENTAGE(정률) / FIXED(정액)
    private String serviceChargeType;

    // 정률(%) 설정
    @DecimalMin(value = "0", message = "봉사료 비율은 0 이상이어야 합니다.")
    @DecimalMax(value = "100", message = "봉사료 비율은 100 이하이어야 합니다.")
    private BigDecimal serviceChargeRate;

    @Min(value = 0, message = "소수점 자릿수는 0 이상이어야 합니다.")
    private Integer serviceChargeDecimalPlaces;

    private String serviceChargeRoundingMethod;

    // 정액(₩) 설정
    @DecimalMin(value = "0", message = "봉사료 정액은 0 이상이어야 합니다.")
    private BigDecimal serviceChargeAmount;
}
