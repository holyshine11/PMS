package com.hola.hotel.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 하우스키핑 작업 수정 요청
 */
@Getter
@NoArgsConstructor
public class HkTaskUpdateRequest {

    private String taskType;
    private String priority;
    private BigDecimal credit;
    private Long assignedTo;
    private String note;
}
