package com.hola.reservation.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 요금 상세 조회 요청
 */
@Getter
@Setter
public class PriceCheckRequest {

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
