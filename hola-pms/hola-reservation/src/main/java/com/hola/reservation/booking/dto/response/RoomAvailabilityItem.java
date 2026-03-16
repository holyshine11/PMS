package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개별 객실 가용성 항목 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomAvailabilityItem {

    private Long roomNumberId;
    private String roomNumber;
    private String descriptionKo;
    private boolean available;

    /** 충돌 예약 정보 */
    private Long conflictReservationId;
    private String conflictReservationNumber;
    private String conflictGuestName;
    private String conflictCheckIn;
    private String conflictCheckOut;
    private String conflictStatus;
}
