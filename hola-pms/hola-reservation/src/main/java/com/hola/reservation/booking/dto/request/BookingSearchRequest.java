package com.hola.reservation.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 가용 객실 검색 요청
 */
@Getter
@Setter
public class BookingSearchRequest {

    @NotNull(message = "체크인 날짜는 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "체크아웃 날짜는 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    @NotNull(message = "성인 인원수는 필수입니다.")
    @Min(value = 1, message = "성인은 1명 이상이어야 합니다.")
    private Integer adults;

    @Min(value = 0, message = "아동 인원수는 0 이상이어야 합니다.")
    private Integer children = 0;
}
