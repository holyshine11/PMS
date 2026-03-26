package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 하우스키핑 설정 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkConfigResponse {

    private Long id;
    private Long propertyId;
    private Boolean inspectionRequired;
    private Boolean autoCreateCheckout;
    private Boolean autoCreateStayover;
    private BigDecimal defaultCheckoutCredit;
    private BigDecimal defaultStayoverCredit;
    private BigDecimal defaultTurndownCredit;
    private BigDecimal defaultDeepCleanCredit;
    private BigDecimal defaultTouchUpCredit;
    private Integer rushThresholdMinutes;
    private Boolean stayoverEnabled;
    private Integer stayoverFrequency;
    private Boolean turndownEnabled;
    private String dndPolicy;
    private Integer dndMaxSkipDays;
    private String dailyTaskGenTime;
    private String odTransitionTime;
}
