package com.hola.hotel.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 하우스키핑 설정 수정 요청
 */
@Getter
@NoArgsConstructor
public class HkConfigUpdateRequest {

    private Boolean inspectionRequired;
    private Boolean autoCreateCheckout;
    private Boolean autoCreateStayover;
    private BigDecimal defaultCheckoutCredit;
    private BigDecimal defaultStayoverCredit;
    private BigDecimal defaultTurndownCredit;
    private BigDecimal defaultDeepCleanCredit;
    private BigDecimal defaultTouchUpCredit;
    private Integer rushThresholdMinutes;
}
