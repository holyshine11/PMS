package com.hola.rate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 레이트 코드 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCodeResponse {

    private Long id;
    private Long propertyId;
    private String rateCode;
    private String rateNameKo;
    private String rateNameEn;
    private String rateCategory;
    private Long marketCodeId;
    private String marketCodeName;
    private String currency;
    private LocalDate saleStartDate;
    private LocalDate saleEndDate;
    private Integer minStayDays;
    private Integer maxStayDays;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 매핑된 객실 타입 ID 목록 */
    private List<Long> roomTypeIds;
}
