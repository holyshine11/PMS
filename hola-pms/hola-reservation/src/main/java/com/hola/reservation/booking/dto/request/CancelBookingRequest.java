package com.hola.reservation.booking.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게스트 자가 취소 요청 DTO
 * email 또는 phone 중 하나로 본인 확인
 */
@Getter
@NoArgsConstructor
public class CancelBookingRequest {

    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    private String phone;
}
