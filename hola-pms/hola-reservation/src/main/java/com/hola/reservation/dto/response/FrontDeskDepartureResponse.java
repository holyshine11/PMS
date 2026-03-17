package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프론트데스크 출발 예정 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrontDeskDepartureResponse {

    private Long masterReservationId;
    private Long subReservationId;
    private String masterReservationNo;
    private String confirmationNo;
    private String guestNameKo;
    private String phoneNumber;
    private String roomTypeName;
    private String roomNumber;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private LocalDateTime actualCheckInTime;
    private Integer adults;
    private Integer children;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance;      // 미결제 잔액
    private Boolean lateCheckOut;
}
