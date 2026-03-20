package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 하우스키핑 작업 일괄 배정 요청 DTO
 */
@Getter
@Setter
public class HkBatchAssignRequest {

    @NotEmpty(message = "작업 ID 목록은 필수입니다.")
    private List<Long> taskIds;

    // null이면 배정 해제
    private Long assignedTo;
}
