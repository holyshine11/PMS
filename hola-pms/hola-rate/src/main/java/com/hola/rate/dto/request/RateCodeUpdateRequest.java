package com.hola.rate.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 레이트 코드 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RateCodeUpdateRequest {

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

    // 숙박유형: OVERNIGHT(기본), DAY_USE
    private String stayType;

    private Boolean useYn;
    private Integer sortOrder;

    /** 매핑할 객실 타입 ID 목록 */
    private List<Long> roomTypeIds;
}
