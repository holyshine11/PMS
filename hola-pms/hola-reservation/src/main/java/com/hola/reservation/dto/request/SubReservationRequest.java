package com.hola.reservation.dto.request;

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

    private Long roomTypeId;
    private Long floorId;
    private Long roomNumberId;
    private Integer adults;
    private Integer children;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Boolean earlyCheckIn;
    private Boolean lateCheckOut;

    // 동반 투숙객
    private List<ReservationGuestRequest> guests;
}
