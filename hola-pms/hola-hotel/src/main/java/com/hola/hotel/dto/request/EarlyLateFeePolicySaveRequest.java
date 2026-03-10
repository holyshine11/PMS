package com.hola.hotel.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 얼리체크인/레이트체크아웃 요금 정책 벌크 저장 요청 DTO
 */
@Getter
@NoArgsConstructor
public class EarlyLateFeePolicySaveRequest {

    @Valid
    @NotNull(message = "정책 목록은 필수입니다.")
    private List<EarlyLateFeePolicyRequest> policies;
}
