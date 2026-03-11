package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 캘린더뷰용 예약 응답 DTO (경량, 이름 마스킹)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCalendarResponse {

    private Long id;
    private String masterReservationNo;
    private String reservationStatus;
    private LocalDate masterCheckIn;
    private LocalDate masterCheckOut;

    /** 마스킹 처리된 투숙객명 */
    private String guestNameMasked;

    /** 층+호수 (예: 12F-1201) */
    private String roomInfo;

    /** 객실타입명 */
    private String roomTypeName;
}
