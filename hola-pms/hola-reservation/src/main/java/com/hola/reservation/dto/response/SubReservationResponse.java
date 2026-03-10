package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 서브 예약 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubReservationResponse {

    private Long id;
    private String subReservationNo;
    private String roomReservationStatus;

    private Long roomTypeId;
    private String roomTypeName;
    private Long floorId;
    private String floorName;
    private Long roomNumberId;
    private String roomNumber;

    private Integer adults;
    private Integer children;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Boolean earlyCheckIn;
    private Boolean lateCheckOut;
    private LocalDateTime actualCheckInTime;
    private LocalDateTime actualCheckOutTime;
    private BigDecimal earlyCheckInFee;
    private BigDecimal lateCheckOutFee;

    // 동반 투숙객
    private List<ReservationGuestResponse> guests;

    // 일별 요금
    private List<DailyChargeResponse> dailyCharges;

    // 서비스 항목
    private List<ReservationServiceResponse> services;
}
