package com.hola.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 상태 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationStatusRequest {

    @NotBlank(message = "변경할 상태값은 필수입니다.")
    private String newStatus;

    // Leg(서브예약) 단위 상태 변경 시 지정. null이면 전체 Leg 일괄 변경 (하위 호환)
    private Long subReservationId;
}
