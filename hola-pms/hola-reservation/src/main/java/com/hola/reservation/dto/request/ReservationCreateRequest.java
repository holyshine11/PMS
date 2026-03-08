package com.hola.reservation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 마스터 예약 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCreateRequest {

    private LocalDate masterCheckIn;
    private LocalDate masterCheckOut;

    // 게스트 정보
    private String guestNameKo;
    private String guestFirstNameEn;
    private String guestMiddleNameEn;
    private String guestLastNameEn;
    private String phoneCountryCode;
    private String phoneNumber;
    private String email;
    private LocalDate birthDate;
    private String gender;
    private String nationality;

    // 예약 관련 코드 (다른 모듈 FK)
    private Long rateCodeId;
    private Long marketCodeId;
    private Long reservationChannelId;

    // 프로모션/OTA
    private String promotionType;
    private String promotionCode;
    private String otaReservationNo;
    private Boolean isOtaManaged;

    private String customerRequest;

    // 서브 예약 목록
    private List<SubReservationRequest> subReservations;
}
