package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 프론트데스크 운영현황 응답 DTO (도착/투숙/출발 공통)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrontDeskOperationResponse {

    // 예약 식별
    private Long reservationId;
    private Long subReservationId;
    private String subReservationNo;
    private String masterReservationNo;
    private String confirmationNo;
    private String reservationStatus;
    private String roomReservationStatus;

    // 게스트 정보
    private String guestNameKo;
    private String guestLastNameEn;
    private String phoneNumber;
    private String email;

    // 객실 정보
    private String roomTypeName;
    private String roomNumber;
    private Long roomNumberId;
    private Integer adults;
    private Integer children;

    // 일정
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer nights;
    private LocalTime eta;
    private LocalTime etd;

    // 체크인/아웃 실적
    private LocalDateTime actualCheckInTime;
    private LocalDateTime actualCheckOutTime;

    // 숙박유형
    private String stayType;
    private LocalTime dayUseStartTime;
    private LocalTime dayUseEndTime;

    // 결제
    private String paymentStatus;

    // 예약 채널
    private String reservationChannelName;
    private Boolean isOtaManaged;
}
