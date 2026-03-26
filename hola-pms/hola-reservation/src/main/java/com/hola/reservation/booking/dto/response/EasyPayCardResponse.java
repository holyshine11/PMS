package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 간편결제 카드 응답 DTO
 */
@Getter
@Builder
public class EasyPayCardResponse {

    private Long id;
    private String email;
    private String cardMaskNo;
    private String issuerName;
    private String cardType;
    private String cardAlias;
    private LocalDateTime createdAt;
}
