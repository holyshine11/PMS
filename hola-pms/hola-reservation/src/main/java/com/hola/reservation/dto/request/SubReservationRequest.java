package com.hola.reservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 서브 예약 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubReservationRequest {

    /** 기존 서브 예약 ID (수정 시 사용, 신규 시 null) */
    private Long id;

    private Long roomTypeId;
    private Long floorId;
    private Long roomNumberId;

    @Min(value = 1, message = "성인 투숙객 수는 1명 이상이어야 합니다.")
    private Integer adults;

    @Min(value = 0, message = "아동 투숙객 수는 0명 이상이어야 합니다.")
    private Integer children;

    @NotNull(message = "서브 예약 체크인 날짜는 필수입니다.")
    private LocalDate checkIn;

    @NotNull(message = "서브 예약 체크아웃 날짜는 필수입니다.")
    private LocalDate checkOut;

    private Boolean earlyCheckIn;
    private Boolean lateCheckOut;

    // 동반 투숙객
    private List<ReservationGuestRequest> guests;
}
