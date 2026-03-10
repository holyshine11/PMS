package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 얼리체크인/레이트체크아웃 요금 정책 단건 요청 DTO
 */
@Getter
@NoArgsConstructor
public class EarlyLateFeePolicyRequest {

    @NotBlank(message = "정책 유형은 필수입니다.")
    @Pattern(regexp = "EARLY_CHECKIN|LATE_CHECKOUT", message = "정책 유형은 EARLY_CHECKIN 또는 LATE_CHECKOUT만 가능합니다.")
    private String policyType;

    @NotBlank(message = "시작 시간은 필수입니다.")
    private String timeFrom;

    @NotBlank(message = "종료 시간은 필수입니다.")
    private String timeTo;

    @NotBlank(message = "요금 유형은 필수입니다.")
    @Pattern(regexp = "PERCENT|FIXED", message = "요금 유형은 PERCENT 또는 FIXED만 가능합니다.")
    private String feeType;

    @NotNull(message = "요금 값은 필수입니다.")
    private BigDecimal feeValue;

    private String description;
}
