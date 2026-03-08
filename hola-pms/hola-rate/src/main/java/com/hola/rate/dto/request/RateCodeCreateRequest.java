package com.hola.rate.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 레이트 코드 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RateCodeCreateRequest {

    @NotBlank(message = "레이트 코드를 입력해주세요.")
    @Size(max = 50, message = "레이트 코드는 50자 이하입니다.")
    private String rateCode;

    @NotBlank(message = "레이트 코드 국문명을 입력해주세요.")
    @Size(max = 200, message = "국문명은 200자 이하입니다.")
    private String rateNameKo;

    @Size(max = 200, message = "영문명은 200자 이하입니다.")
    private String rateNameEn;

    @NotBlank(message = "레이트 카테고리를 선택해주세요.")
    private String rateCategory;

    private Long marketCodeId;

    @NotBlank(message = "통화를 선택해주세요.")
    private String currency;

    @NotNull(message = "판매 시작일을 입력해주세요.")
    private LocalDate saleStartDate;

    @NotNull(message = "판매 종료일을 입력해주세요.")
    private LocalDate saleEndDate;

    @NotNull(message = "최소 숙박일수를 입력해주세요.")
    @Min(value = 1, message = "최소 숙박일수는 1 이상이어야 합니다.")
    private Integer minStayDays;

    @NotNull(message = "최대 숙박일수를 입력해주세요.")
    @Min(value = 1, message = "최대 숙박일수는 1 이상이어야 합니다.")
    private Integer maxStayDays;

    private Boolean useYn;
    private Integer sortOrder;

    /** 매핑할 객실 타입 ID 목록 */
    private List<Long> roomTypeIds;
}
