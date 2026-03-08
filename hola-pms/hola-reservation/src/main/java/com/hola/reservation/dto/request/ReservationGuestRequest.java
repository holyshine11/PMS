package com.hola.reservation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동반 투숙객 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationGuestRequest {

    private Integer guestSeq;
    private String guestNameKo;
    private String guestFirstNameEn;
    private String guestMiddleNameEn;
    private String guestLastNameEn;
}
