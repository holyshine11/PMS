package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * VAN 취소에 필요한 원거래 정보 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VanCancelInfoResponse {

    private Long transactionId;
    private Long workstationId;
    private String authCode;
    private String rrn;
    private BigDecimal amount;
    private String sequenceNo;
    private String paymentMethod;
    private String wsNo;
    private String kpspHost;
    private Integer kpspPort;
}
