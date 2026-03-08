package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 리스트/카드뷰용 응답 DTO (경량)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationListResponse {

    private Long id;
    private String masterReservationNo;
    private String confirmationNo;
    private String reservationStatus;
    private LocalDate masterCheckIn;
    private LocalDate masterCheckOut;
    private String guestNameKo;
    private String phoneNumber;

    // 첫 번째 서브 예약 기준 객실 정보
    private String roomTypeName;
    private String roomNumber;

    private String reservationChannelName;
    private Boolean isOtaManaged;
    private LocalDateTime createdAt;
}
