package com.hola.reservation.booking.pg.kicc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * KICC 거래등록 요청 (POST /api/ep9/trades/webpay)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiccRegisterRequest {

    /** KICC 상점ID */
    private String mallId;

    /** 상점 주문번호 (최대 40자, 영문/숫자/-/_) */
    private String shopOrderNo;

    /** 결제 금액 */
    private Long amount;

    /** 결제수단 코드 ("00"=미지정, "11"=신용카드, "81"=정기결제) */
    private String payMethodTypeCode;

    /** 통화 ("00"=원화) */
    private String currency;

    /** 인증완료 후 리턴 URL */
    private String returnUrl;

    /** 디바이스 ("pc" / "mobile") */
    private String deviceTypeCode;

    /** 클라이언트 타입 ("00"=통합형) */
    @Builder.Default
    private String clientTypeCode = "00";

    /** 주문 정보 */
    private OrderInfo orderInfo;

    /** 결제수단 설정 */
    private PayMethodInfo payMethodInfo;

    /** 복합과세 정보 */
    private TaxInfo taxInfo;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderInfo {
        private String goodsName;
        private CustomerInfo customerInfo;

        @Getter
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class CustomerInfo {
            private String customerName;
            private String customerContactNo;
            private String customerEmail;
        }
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PayMethodInfo {
        private CardMethodInfo cardMethodInfo;
        private BillKeyMethodInfo billKeyMethodInfo;

        @Getter
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class CardMethodInfo {
            private Integer installmentMonth;
            private String noInterestMonth;
            private String cardCd;
        }

        @Getter
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class BillKeyMethodInfo {
            /** 카드인증 타입 ("0"=전체, "1"=번호+유효기간, "2"=번호+유효기간+생년월일) */
            private String certType;
            /** 빌키 (빌키 결제 시) */
            private String batchKey;
        }
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaxInfo {
        private Long taxAmount;
        private Long taxFreeAmount;
        private Long vatAmount;
    }
}
