package com.hola.reservation.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 예약 수정 요청 (게스트 자가 수정)
 */
@Getter
@NoArgsConstructor
public class BookingModifyRequest {

    /** 이메일 본인인증 */
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    /** 변경할 체크인 날짜 */
    @NotNull(message = "체크인 날짜는 필수입니다.")
    private LocalDate checkIn;

    /** 변경할 체크아웃 날짜 */
    @NotNull(message = "체크아웃 날짜는 필수입니다.")
    private LocalDate checkOut;

    /** 변경할 성인 수 */
    private Integer adults;

    /** 변경할 아동 수 */
    private Integer children;

    /** 게스트 한글 이름 변경 */
    private String guestNameKo;

    /** 전화번호 변경 */
    private String phoneNumber;

    /** 요청사항 변경 */
    private String customerRequest;
}
