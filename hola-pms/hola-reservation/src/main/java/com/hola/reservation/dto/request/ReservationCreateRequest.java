package com.hola.reservation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "체크인 날짜는 필수입니다.")
    private LocalDate masterCheckIn;

    @NotNull(message = "체크아웃 날짜는 필수입니다.")
    private LocalDate masterCheckOut;

    // 게스트 정보
    @NotBlank(message = "투숙객 이름은 필수입니다.")
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

    // 서브 예약 목록 (최소 1개 필수)
    @NotEmpty(message = "객실 레그는 최소 1개 이상 필요합니다.")
    @Valid
    private List<SubReservationRequest> subReservations;
}
