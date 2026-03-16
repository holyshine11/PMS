package com.hola.reservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 시 유료 서비스 선택 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceSelectionRequest {

    @NotNull(message = "서비스 옵션 ID는 필수입니다.")
    private Long serviceOptionId;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    @Builder.Default
    private Integer quantity = 1;
}
