package com.hola.reservation.booking.pg.kicc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * KICC 빌키 결제 승인 요청 (POST /api/trades/approval/batch)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiccBatchApprovalRequest {

    /** KICC 상점ID */
    private String mallId;

    /** 멱등성 키 (UUID) */
    private String shopTransactionId;

    /** 상점 주문번호 */
    private String shopOrderNo;

    /** 승인요청일 (yyyyMMdd) */
    private String approvalReqDate;

    /** 결제금액 */
    private Long amount;

    /** 통화 ("00"=원화) */
    @Builder.Default
    private String currency = "00";

    /** 주문 정보 */
    private OrderInfo orderInfo;

    /** 결제수단 정보 */
    private PayMethodInfo payMethodInfo;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderInfo {
        private String goodsName;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PayMethodInfo {
        private BillKeyMethodInfo billKeyMethodInfo;
        private CardMethodInfo cardMethodInfo;

        @Getter
        @Builder
        public static class BillKeyMethodInfo {
            private String batchKey;
        }

        @Getter
        @Builder
        public static class CardMethodInfo {
            @Builder.Default
            private Integer installmentMonth = 0;
        }
    }
}
