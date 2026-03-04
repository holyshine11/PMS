package com.hola.hotel.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 취소 수수료 벌크 저장 요청 DTO
 */
@Getter
@NoArgsConstructor
public class CancellationFeeSaveRequest {

    @Valid
    @NotEmpty(message = "취소 수수료 항목은 최소 1건 이상이어야 합니다.")
    private List<CancellationFeeRequest> fees;
}
