package com.hola.reservation.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 생성 요청 (게스트용)
 */
@Getter
@Setter
public class BookingCreateRequest {

    /** 중복 요청 방지 키 (UUID) */
    @NotBlank(message = "idempotencyKey는 필수입니다.")
    @Size(max = 50)
    private String idempotencyKey;

    @NotNull(message = "투숙객 정보는 필수입니다.")
    @Valid
    private GuestInfo guest;

    @NotEmpty(message = "객실 선택은 필수입니다.")
    @Valid
    private List<RoomSelection> rooms;

    @NotNull(message = "결제 정보는 필수입니다.")
    @Valid
    private PaymentInfo payment;

    @AssertTrue(message = "이용약관에 동의해야 합니다.")
    private boolean agreedTerms;

    @Getter
    @Setter
    public static class GuestInfo {

        @NotBlank(message = "투숙객 이름(한글)은 필수입니다.")
        @Size(max = 50)
        private String guestNameKo;

        @Size(max = 50)
        private String guestFirstNameEn;

        @Size(max = 50)
        private String guestLastNameEn;

        private String phoneCountryCode = "+82";

        @NotBlank(message = "전화번호는 필수입니다.")
        @Size(max = 20)
        private String phoneNumber;

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100)
        private String email;

        @Size(max = 10)
        private String nationality = "KR";
    }

    @Getter
    @Setter
    public static class RoomSelection {

        @NotNull(message = "객실 타입은 필수입니다.")
        private Long roomTypeId;

        @NotNull(message = "레이트 코드는 필수입니다.")
        private Long rateCodeId;

        @NotNull(message = "체크인 날짜는 필수입니다.")
        private LocalDate checkIn;

        @NotNull(message = "체크아웃 날짜는 필수입니다.")
        private LocalDate checkOut;

        @NotNull(message = "성인 인원수는 필수입니다.")
        @Min(value = 1, message = "성인은 1명 이상이어야 합니다.")
        private Integer adults;

        @Min(value = 0, message = "아동 인원수는 0 이상이어야 합니다.")
        private Integer children = 0;
    }

    @Getter
    @Setter
    public static class PaymentInfo {

        @NotBlank(message = "결제 수단은 필수입니다.")
        private String method = "CARD";

        @Size(max = 20)
        private String cardNumber;

        @Size(max = 5)
        private String expiryDate;

        @Size(max = 4)
        private String cvv;
    }
}
