package com.hola.reservation.booking.pg.kicc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * KICC 빌키 삭제 요청 (POST /api/trades/removeBatchKey)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiccRemoveKeyRequest {

    /** KICC 상점ID */
    private String mallId;

    /** 멱등성 키 (UUID) */
    private String shopTransactionId;

    /** 삭제할 빌키 */
    private String batchKey;

    /** 삭제요청일 (yyyyMMdd) */
    private String removeReqDate;
}
