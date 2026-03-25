package com.hola.reservation.booking.pg.kicc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * KICC 결제승인 / 빌키발급 요청 (POST /api/ep9/trades/approval)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiccApprovalRequest {

    /** KICC 상점ID */
    private String mallId;

    /** 멱등성 키 (UUID) */
    private String shopTransactionId;

    /** 인증 거래번호 (결제창에서 수신) */
    private String authorizationId;

    /** 상점 주문번호 */
    private String shopOrderNo;

    /** 승인요청일 (yyyyMMdd) */
    private String approvalReqDate;
}
