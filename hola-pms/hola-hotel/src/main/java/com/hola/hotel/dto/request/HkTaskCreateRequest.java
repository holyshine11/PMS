package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 하우스키핑 작업 생성 요청
 */
@Getter
@NoArgsConstructor
public class HkTaskCreateRequest {

    @NotNull(message = "객실 ID를 입력해주세요.")
    private Long roomNumberId;

    @NotBlank(message = "작업 유형을 선택해주세요.")
    private String taskType;  // CHECKOUT, STAYOVER, TURNDOWN, DEEP_CLEAN, TOUCH_UP

    private String priority;  // RUSH, HIGH, NORMAL, LOW
    private BigDecimal credit;
    private Long assignedTo;  // 배정할 하우스키퍼 ID
    private String note;
    private LocalDateTime nextCheckinAt;
}
