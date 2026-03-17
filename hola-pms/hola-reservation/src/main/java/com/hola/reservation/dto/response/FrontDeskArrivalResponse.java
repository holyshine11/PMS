package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프론트데스크 도착 예정 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrontDeskArrivalResponse {

    private Long masterReservationId;
    private Long subReservationId;
    private String masterReservationNo;
    private String confirmationNo;
    private String guestNameKo;
    private String phoneNumber;
    private String roomTypeName;
    private String roomNumber;       // 배정된 객실 (null 가능)
    private Long roomNumberId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer adults;
    private Integer children;
    private Long roomTypeId;         // 객실 배정 시 기존 타입 유지용
    private String reservationStatus;
    private String channelName;
    private BigDecimal totalAmount;  // 총 요금
    private String hkStatus;         // 배정 객실의 HK 상태
}
