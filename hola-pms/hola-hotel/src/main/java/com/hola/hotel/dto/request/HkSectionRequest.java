package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 하우스키핑 구역 생성/수정 요청 DTO
 */
@Getter
@Setter
public class HkSectionRequest {

    @NotBlank(message = "구역 이름은 필수입니다.")
    private String sectionName;

    private String sectionCode;

    private BigDecimal maxCredits;

    /** 포함할 층 ID 목록 */
    private List<Long> floorIds;

    /** 기본 담당자 ID 목록 */
    private List<Long> housekeeperIds;
}
