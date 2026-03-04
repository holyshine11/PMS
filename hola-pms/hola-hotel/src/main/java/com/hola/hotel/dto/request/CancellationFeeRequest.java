package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 취소 수수료 단건 행 요청 DTO
 */
@Getter
@NoArgsConstructor
public class CancellationFeeRequest {

    @NotBlank(message = "체크인 기준은 필수입니다.")
    @Pattern(regexp = "DATE|NOSHOW", message = "체크인 기준은 DATE 또는 NOSHOW만 가능합니다.")
    private String checkinBasis;

    private Integer daysBefore;

    @NotNull(message = "수수료 금액은 필수입니다.")
    private BigDecimal feeAmount;

    @NotBlank(message = "수수료 유형은 필수입니다.")
    @Pattern(regexp = "PERCENTAGE|FIXED_KRW|FIXED_USD", message = "수수료 유형은 PERCENTAGE, FIXED_KRW, FIXED_USD만 가능합니다.")
    private String feeType;
}
