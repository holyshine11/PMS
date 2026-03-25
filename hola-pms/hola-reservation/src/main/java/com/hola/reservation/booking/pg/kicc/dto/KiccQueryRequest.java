package com.hola.reservation.booking.pg.kicc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * KICC 거래상태 조회 요청 (POST /api/trades/retrieveTransaction)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiccQueryRequest {

    /** KICC 상점ID */
    private String mallId;

    /** 원거래 멱등성 키 */
    private String shopTransactionId;

    /** 거래일 (yyyyMMdd) */
    private String transactionDate;
}
