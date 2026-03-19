package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 호수별 가용성 응답 DTO (층/호수 배정 모달용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomNumberAvailabilityResponse {

    private Long id;
    private String roomNumber;
    private String descriptionKo;

    /** 가용 여부 */
    private boolean available;

    /** OOO/OOS 구분 (해당 시) */
    private String unavailableType;

    /** 선 예약 번호 (불가 시) */
    private String conflictReservationNo;

    /** 선 예약자명 (마스킹) */
    private String conflictGuestName;

    /** 선 예약 체크인 */
    private LocalDate conflictCheckIn;

    /** 선 예약 체크아웃 */
    private LocalDate conflictCheckOut;
}
