package com.hola.reservation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 예약 유료 서비스 추가 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationServiceRequest {

    /** 유료 서비스 옵션 ID (필수) */
    private Long serviceOptionId;

    /** 수량 (기본 1) */
    private Integer quantity;

    /** 서비스 적용일 (선택) */
    private LocalDate serviceDate;
}
