package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 상세/수정폼용 응답 DTO (전체)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDetailResponse {

    private Long id;
    private Long propertyId;
    private String masterReservationNo;
    private String confirmationNo;
    private String reservationStatus;
    private LocalDate masterCheckIn;
    private LocalDate masterCheckOut;
    private LocalDateTime reservationDate;

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

    // 다른 모듈 FK
    private Long rateCodeId;
    private Long marketCodeId;
    private String rateCodeName;
    private String marketCodeName;
    private Long reservationChannelId;

    // 레이트코드 기반 stayType (OVERNIGHT / DAY_USE)
    private String stayType;

    // 프로모션/OTA
    private String promotionType;
    private String promotionCode;
    private String otaReservationNo;
    private Boolean isOtaManaged;
    private String customerRequest;

    // 서브 예약
    private List<SubReservationResponse> subReservations;

    // 보증금
    private List<ReservationDepositResponse> deposits;

    // 결제 요약
    private PaymentSummaryResponse payment;

    // 메모
    private List<ReservationMemoResponse> memos;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
