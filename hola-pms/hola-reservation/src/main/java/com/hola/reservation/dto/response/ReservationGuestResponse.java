package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동반 투숙객 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationGuestResponse {

    private Long id;
    private Integer guestSeq;
    private String guestNameKo;
    private String guestFirstNameEn;
    private String guestMiddleNameEn;
    private String guestLastNameEn;
}
