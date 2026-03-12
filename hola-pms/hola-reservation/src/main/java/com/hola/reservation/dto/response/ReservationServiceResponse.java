package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 예약 서비스 항목 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationServiceResponse {

    private Long id;
    private String serviceType;
    private Long serviceOptionId;
    private String serviceName;
    private LocalDate serviceDate;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal tax;
    private BigDecimal totalPrice;
}
