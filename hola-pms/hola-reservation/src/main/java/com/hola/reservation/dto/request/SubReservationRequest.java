package com.hola.reservation.dto.request;

import jakarta.validation.Valid;
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

    @NotNull(message = "객실 타입은 필수입니다.")
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

    // Dayuse 관련 (null이면 레이트코드에서 자동 결정)
    private String stayType;
    private Integer dayUseDurationHours;

    // 동반 투숙객
    @Valid
    private List<ReservationGuestRequest> guests;

    // 유료 서비스 선택 (Add-on)
    private List<ServiceSelectionRequest> services;
}
